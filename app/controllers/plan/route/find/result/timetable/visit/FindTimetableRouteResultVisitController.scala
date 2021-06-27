package controllers.plan.route.find.result.timetable.visit

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, LocalTime, Period}
import java.util.Date
import java.util.concurrent.TimeUnit

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import controllers.plan.route.find.result.FindRouteResultHelper.mkTime
import controllers.plan.route.find.result.{FindRouteResultHelper, ResultViewModel, Waypoint}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.data.Event
import models.location.{Location, MapLocation}
import models.plan.timetable.trains.TrainTimetableService
import models.route.Route
import models.route.display.map.MapRoute
import models.timetable.model.train.IndividualTimetableLocation
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.plan.pointtopoint.{Path, PointToPointRouteFinderService}
import services.visit.event.EventService
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@Singleton
class FindTimetableRouteResultVisitController @Inject()(
                                     cc: ControllerComponents,
                                     authenticatedUserAction: AuthorizedWebAction,
                                     pathService: PointToPointRouteFinderService,
                                     locationsService: LocationVisitService,
                                     routesService: RouteVisitService,
                                     eventService: EventService,
                                     timetableService: TrainTimetableService,
                                     jwtService: JWTService

                                   ) extends AbstractController(cc) {

  def visit(trainUid: String, date: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {

      val token = jwtService.createToken(request.user, new Date())

      val data = request.request.body.asFormUrlEncoded

      val overrideStartTime = FindRouteResultHelper.extractBooleanFromData(data, "overrideStartDateAndTime")

      var messages = List.empty[String]


      val setVisitDetails = FindRouteResultHelper.extractBooleanFromData(data, "overrideVisitDetails")
      val includeNonPublicStops = FindRouteResultHelper.extractBooleanFromData(data, "includeNonPublicStopsInVisit")
      val visitMode = FindRouteResultHelper.extractString(data, "visitMode")
      val fromLocationIndex: Int = FindRouteResultHelper.extractInt(data, "from") - 1
      val toLocationIndex: Int = FindRouteResultHelper.extractInt(data, "to")
      val overriddenStartDate = FindRouteResultHelper.extractString(data, "startDate")
      val startTime = FindRouteResultHelper.extractString(data, "startTime")
      val visitName = FindRouteResultHelper.extractString(data, "visitName")
      val overriddenTrainUid: Option[String] = FindRouteResultHelper.extractString(data, "trainUid")
      val trainHeadcode = FindRouteResultHelper.extractString(data, "trainHeadcode")
      val debug = FindRouteResultHelper.extractBooleanFromData(data, "debug")

      if(trainUid.isEmpty || trainUid.isBlank) messages = "Please specify a train UID" :: messages
      val (year, month, day) = {
        try {
          val d = LocalDate.parse(date)
          (d.getYear, d.getMonthValue, d.getDayOfMonth)
        }
        catch {
          case e: Throwable =>
            messages =  s"Please specify a valid date in the format year-month-day, or leave blank for today - $e" :: messages
            (LocalDate.now.getYear, LocalDate.now.getMonthValue, LocalDate.now.getDayOfMonth)
        }
      }

      val timetableF = timetableService
        .getTrain(trainUid, year.toString, month.toString, day.toString)

      val timetableOpt = Await.result(timetableF, Duration(30, TimeUnit.SECONDS))

      if(timetableOpt.isEmpty) {
        messages = s"Timetable for train ${trainUid} on date ${date} could not be found. Unable to visit today. Please try again" :: messages
        play.api.mvc.Results.NotFound(views.html.plan.route.find.timetable.index(
          request.user,
          token,
          trainUid,
          date,
          controllers.plan.route.find.result.timetable.routes.TimetableFindRouteResultController.timetable(trainUid, date),
          messages
        ))
      }
      else {


        val timetable = timetableOpt.get

        val departTime = timetable.locations.headOption.flatMap(_.departure).getOrElse(LocalTime.now)
        val startDate = LocalDate.parse(date)
        val startDateTime = LocalDateTime.of(startDate, departTime)


        val eventDuration = {
          val arrivalTime = timetable.locations.lastOption.flatMap(_.arrival).getOrElse(LocalTime.now)
          val endDate = if(arrivalTime.isBefore(departTime)) startDate.plusDays(1) else startDate
          val endDateTime = LocalDateTime.of(endDate, arrivalTime)

          val diff = java.time.Duration.between(startDateTime, endDateTime)
          diff
        }

        val path = pathService.findRouteForWaypoints(timetable.locations.map(_.tiploc))
        val routes = path.routes
        val locations = path.locations
        val publicStops = timetable.locations.filter(l => l.publicDeparture.isDefined || l.publicArrival.isDefined)

        val visitStartTime = makeVisitStartDateAndTime(overrideStartTime, overriddenStartDate, startTime, startDateTime)
        val (locationsToVisit, routesToVisit) = calculateRoutesAndLocationsToVisit(visitMode, routes, locations, publicStops, fromLocationIndex, toLocationIndex, includeNonPublicStops)

        val event: Event = makeEvent(request,
          locationsToVisit.headOption.map(_.name).getOrElse("Nowhere"),
          locationsToVisit.reverse.headOption.map(_.name).getOrElse("Nowhere"),
          setVisitDetails,
          overrideStartTime,
          visitStartTime,
          visitName,
          eventDuration.toSeconds)


        if(!(eventService.hasActiveEvent(request.user) && eventService.getActiveEvent(request.user).get.id == event.id)) {
          eventService.saveEvent(event, request.user)
        }

        val debugStrBuf = new StringBuffer()
        val debugInfo =
          s"""
             |*********************************
             |* Timetable based Visit Options *
             |*********************************
             |
             |ROUTING OPTIONS
             | TRAIN UID : $trainUid
             | DATE      : $date
             | FREIGHT   : true
             | FIXED     : true
             | UNKNOWN   : true
             |
             |QUICK VISIT OPTIONS
             | VISIT MODE         : $visitMode
             | FROM LOCATION INDEX: $fromLocationIndex
             | FROM LOCATION ID   : ${locations(fromLocationIndex).id}
             | TO LOCATION INDEX  : $toLocationIndex
             | TO LOCATION ID     : ${locations(toLocationIndex).id}
             | INCLUDE NON PUBLIC : $includeNonPublicStops
             |
             |CALCULATED ROUTE VISIT DETAILS
             | LOCATIONS TO VISIT : ${locationsToVisit.map(_.id).zipWithIndex}
             | ROUTES TO VISIT    : ${routesToVisit.map(r => r.from.id + "-" + r.to.id).zipWithIndex}
             |
             |SET START DATE AND TIME
             | OVERRIDE           : $overrideStartTime
             | VISIT START TIME   : $visitStartTime
             |
             |SET VISIT DETAILS
             | OVERRIDE           : $setVisitDetails
             | VISIT NAME         : $visitName
             | TRAIN UID          : $trainUid
             | TRAIN HEADCODE     : $trainHeadcode
             | """.stripMargin

        debugStrBuf.append(debugInfo).append("\n")


        var lastVisitTime: LocalDateTime = visitStartTime

        var remainingRoutesToVisit = routesToVisit
        val nextLocationsToVisit: List[Location] = locationsToVisit.tail

        locationsToVisit.zipWithIndex.foreach(l => {
          val (nextLocation, index) = l
          val routes = {
            var routeVisitTime: LocalDateTime = lastVisitTime
            val startLocation = nextLocation
            val endLocation = if(index >= nextLocationsToVisit.size) nextLocation else nextLocationsToVisit(index)
            val nextRoutes: List[Route] = {
              var routes = List.empty[Route]
              //get first element from route list
              if(remainingRoutesToVisit.nonEmpty) {
                val route = remainingRoutesToVisit.head
                routes = routes :+ route
                remainingRoutesToVisit = remainingRoutesToVisit.filterNot(r => r.equals(route))
              }

              //get the rest of the routes if any
              while(
                remainingRoutesToVisit.nonEmpty
                  && !(remainingRoutesToVisit.head.from.id.equals(endLocation.id) || remainingRoutesToVisit.head.to.id.equals(endLocation.id))) {
                val route = remainingRoutesToVisit.head
                routes = routes :+ route
                remainingRoutesToVisit = remainingRoutesToVisit.filterNot(r => r.equals(route))
              }
              routes.foreach(r => {
                remainingRoutesToVisit = remainingRoutesToVisit.filterNot(rr => r.equals(rr))
              })

              routes.foreach(r => {
                routesService.visitRoute(r, routeVisitTime, routeVisitTime, "", request.user)
                val debugVisitRoute = s"VISIT ROUTE   : Visiting route ${r.from.id}-${r.to.id} with visit time $routeVisitTime"
                debugStrBuf.append(debugVisitRoute).append("\n")
                routeVisitTime = routeVisitTime.plusSeconds(r.travelTimeInSeconds.getSeconds)

              })
              routes

            }
            val travelTime = nextRoutes.map(_.travelTimeInSeconds).map(_.toSeconds).sum
            val visitDescription = ""
            //work out when the timetable says the train should arrive
            val timetableScheduledArrivalTimeOpt = timetable.locations.find(_.tiploc.equals(nextLocation.id)).flatMap(_.publicArrival)
            //if the arrival time is before the last visit time, we've probably crossed midnight so add one day
            val timetableScheduledArrivalDateTimeOpt = timetableScheduledArrivalTimeOpt.map(timetabledTime => {
              val time = lastVisitTime.toLocalTime
              val date = lastVisitTime.toLocalDate
              val timetabledDate = if(timetabledTime.isBefore(time)) date.plusDays(1) else date
              val timetabledDateTime = LocalDateTime.of(timetabledDate, timetabledTime)
              timetabledDateTime
            })
            val locationVisitTime: LocalDateTime = timetableScheduledArrivalDateTimeOpt.getOrElse(lastVisitTime.plusSeconds(travelTime))
            val debugVisitLocation = s"VISIT LOCATION: Visiting Index $index,  location ${nextLocation.id}, VisitTime $locationVisitTime"
            debugStrBuf.append(debugVisitLocation).append("\n")
            locationsService.visitLocation(nextLocation, locationVisitTime, locationVisitTime, visitDescription , request.user)

            lastVisitTime = locationVisitTime
          }

        })

        val mapLocationList = path.locations.map(MapLocation(_))

        val mapRouteList = routes.map(r => MapRoute(r))
        val routeList = routes
        val waypoints = locations.map(l => Waypoint(l.id, l.name, timetable.locations.find(tl => tl.tiploc.equals(l.id)).exists(tl => tl.publicDeparture.isDefined || tl.publicArrival.isDefined)))

        val distance = path.routes.map(_.distance).sum
        val time = eventDuration.getSeconds


        messages = if(debug) messages ++ debugStrBuf.toString.split("\n").toList else messages

        play.api.mvc.Results.Ok(views.html.plan.route.find.timetable.result.index(
          request.user,
          token,
          ResultViewModel(
            mapLocationList,
            mapRouteList,
            routeList,
            waypoints,
            path.followFreightLinks,
            path.followFixedLinks,
            path.followUnknownLinks,
            distance,
            mkTime(time, " (timetabled)"),
            true,
            false,
            false,
            None,
            Some(LocalDate.now),
            None,
            None,
            visitMode.getOrElse("visitAllRoutesAndPublicStops"),
            fromLocationIndex,
            toLocationIndex - 1,
            includeNonPublicStops,
            overrideStartTime,
            overriddenStartDate.getOrElse(date),
            startTime.getOrElse(""),
            setVisitDetails,
            visitName.getOrElse(""),
            trainUid,
            trainHeadcode.getOrElse(""),
            locationsToVisit,
            routesToVisit,
            controllers.plan.route.find.result.timetable.visit.routes.FindTimetableRouteResultVisitController.visit(trainUid, date),
            controllers.plan.route.find.timetable.routes.TimetableRouteController.index(trainUid, date)
          ),
          messages))
        }
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  private def makeVisitStartDateAndTime(overrideStartTime: Boolean, startDate: Option[String], startTime: Option[String], timetabledStartTime: LocalDateTime): LocalDateTime = {
    if (overrideStartTime) {
      val startDateStr = startDate.getOrElse(LocalDate.now.format(DateTimeFormatter.ISO_LOCAL_DATE))
      val startTimeStr = startTime
        .map(s => {
          if (s.length == 1) s"0$s:00:00"
          else if (s.length == 2) s"$s:00:00"
          else if (s.length == 3 && !s.contains(":")) s"${s.charAt(0)}:${s.charAt(1)}${s.charAt(1)}:00"
          else if (s.length == 4 && !s.contains(":")) s"${s.charAt(0)}${s.charAt(1)}:${s.charAt(2)}${s.charAt(3)}:00"
          else s
        })
        .getOrElse(LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)) //format hh:mm:ss

      val dateTimeStr = s"${startDateStr}T$startTimeStr"
      val time = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      time
    } else timetabledStartTime
  }

  private def calculateRoutesAndLocationsToVisit(
                                                  visitMode: Option[String],
                                                  routes: List[Route],
                                                  locations: List[Location],
                                                  publicLocations: List[IndividualTimetableLocation],
                                                  fromLocationIndex: Int,
                                                  toLocationIndex: Int,
                                                  includeNonPublicStops: Boolean): (List[Location], List[Route]) = {
    val pathLocations = locations
    val pathRoutes = routes
    val publicLocationIds = publicLocations.map(_.tiploc)

    visitMode match {
      case Some("visitAllRoutesAndPublicStopsBetweenLocations") =>

        val splicedLocations = pathLocations.slice(fromLocationIndex, toLocationIndex + 1)
        val setOfLocationsToVisit = if (!includeNonPublicStops) splicedLocations.filter(l => publicLocationIds.contains(l.id)) else splicedLocations

        val routeStartIndex = fromLocationIndex
        val routeEndIndex = toLocationIndex - 1
        val splicedRoutes = pathRoutes.slice(routeStartIndex, routeEndIndex + 1)
        val setOfRoutesToVisit = splicedRoutes

        (setOfLocationsToVisit, setOfRoutesToVisit)

      case Some("visitAllRoutesAndPublicStops") =>
        val setOfRoutesToVisit = pathRoutes
        val setOfLocationsToVisit = if (!includeNonPublicStops) pathLocations.filter(l => publicLocationIds.contains(l.id)) else pathLocations

        (setOfLocationsToVisit, setOfRoutesToVisit)

      case Some("visitAllRoutes") =>
        val setOfRoutesToVisit = pathRoutes
        val setOfLocationsToVisit = List.empty

        (setOfLocationsToVisit, setOfRoutesToVisit)

      case Some("visitAllPublicStops") =>
        val setOfLocationsToVisit = if (!includeNonPublicStops) pathLocations.filter(l => publicLocationIds.contains(l.id)) else pathLocations
        val setOfRoutesToVisit = List.empty

        (setOfLocationsToVisit, setOfRoutesToVisit)

      case _ =>
        val setOfLocationsToVisit = List.empty
        val setOfRoutesToVisit = List.empty

        (setOfLocationsToVisit, setOfRoutesToVisit)


    }
  }

  private def makeEvent(request: WebUserContext[AnyContent],
                        from: String,
                        to: String,
                        setVisitDetails: Boolean,
                        setVisitTime: Boolean,
                        eventStartTime: LocalDateTime,
                        visitName: Option[String],
                        eventDuration: Long): Event = {
    val defaultEventName = eventStartTime.format(DateTimeFormatter.ISO_DATE) + s" - $from to $to"
    val defaultEventSummary = s"Visit from ${from} to ${to}"

    if (setVisitDetails || setVisitTime) {
      makeEvent0(name = visitName.getOrElse(defaultEventName),
        startedAt = eventStartTime,
        endedAt = eventStartTime.plusSeconds(eventDuration),
        details = defaultEventSummary)
    } else if (eventService.hasActiveEvent(request.user)) {
      eventService.getActiveEvent(request.user).get
    } else {
      makeEvent0(name = defaultEventName,
        startedAt = eventStartTime,
        endedAt = eventStartTime.plusSeconds(eventDuration),
        details = defaultEventSummary)
    }
  }

  def makeEvent0(name: String, startedAt: LocalDateTime, endedAt: LocalDateTime, details: String): Event =
    Event(name = name,
    startedAt = startedAt,
    endedAt = endedAt,
    details = details)


}

