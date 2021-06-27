package controllers.plan.route.find.result.pointtopoint.visit

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import controllers.plan.route.find.result.FindRouteResultHelper.mkTime
import controllers.plan.route.find.result.{FindRouteResultHelper, ResultViewModel, Waypoint}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.data.Event
import models.location.{Location, MapLocation}
import models.route.Route
import models.route.display.map.MapRoute
import services.plan.pointtopoint.{Path, PointToPointRouteFinderService}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.visit.event.EventService
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

@Singleton
class FindPointToPointRouteResultVisitController @Inject()(
                                     cc: ControllerComponents,
                                     authenticatedUserAction: AuthorizedWebAction,
                                     pathService: PointToPointRouteFinderService,
                                     locationsService: LocationVisitService,
                                     routesService: RouteVisitService,
                                     eventService: EventService,
                                     jwtService: JWTService

                                   ) extends AbstractController(cc) {

  def visit() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {

      val token = jwtService.createToken(request.user, new Date())

      val data = request.request.body.asFormUrlEncoded

      val locationStrToRouteVia: List[String] = FindRouteResultHelper.extractStringListFromData(data, "waypoints")
      val followFixedLinks: Boolean = FindRouteResultHelper.extractBooleanFromData(data, "followFixedLinks")
      val followFreightLinks: Boolean = FindRouteResultHelper.extractBooleanFromData(data, "followFreightLinks")
      val followUnknownLinks: Boolean = FindRouteResultHelper.extractBooleanFromData(data, "followUnknownLinks")

      val path: Path = pathService.findRouteForWaypoints(locationStrToRouteVia, followFixedLinks, followFreightLinks, followUnknownLinks)

      val overrideStartTime = FindRouteResultHelper.extractBooleanFromData(data, "overrideStartDateAndTime")
      val setVisitDetails = FindRouteResultHelper.extractBooleanFromData(data, "overrideVisitDetails")
      val includeNonPublicStops = FindRouteResultHelper.extractBooleanFromData(data, "includeNonPublicStopsInVisit")
      val visitMode = FindRouteResultHelper.extractString(data, "visitMode")
      val fromLocationIndex: Int = FindRouteResultHelper.extractInt(data, "from") - 1
      val toLocationIndex: Int = FindRouteResultHelper.extractInt(data, "to")
      val startDate = FindRouteResultHelper.extractString(data, "startDate")
      val startTime = FindRouteResultHelper.extractString(data, "startTime")
      val visitName = FindRouteResultHelper.extractString(data, "visitName")
      val trainUid: Option[String] = FindRouteResultHelper.extractString(data, "trainUid")
      val trainHeadcode = FindRouteResultHelper.extractString(data, "trainHeadcode")
      val debug = FindRouteResultHelper.extractBooleanFromData(data, "debug")

      val eventDuration = path.routes.map(_.travelTimeInSeconds).map(_.getSeconds).sum

      val visitStartTime = makeVisitStartDateAndTime(overrideStartTime, startDate, startTime)
      val (locationsToVisit, routesToVisit) = calculateRoutesAndLocationsToVisit(visitMode, path, fromLocationIndex, toLocationIndex, includeNonPublicStops)

      val event: Event = makeEvent(request,
        locationsToVisit.headOption.map(_.name).getOrElse("Nowhere"),
        locationsToVisit.reverse.headOption.map(_.name).getOrElse("Nowhere"),
        setVisitDetails,
        overrideStartTime,
        visitStartTime,
        visitName,
        eventDuration)


      if(!(eventService.hasActiveEvent(request.user) && eventService.getActiveEvent(request.user).get.id == event.id)) {
        eventService.saveEvent(event, request.user)
      }

      val debugStrBuf = new StringBuffer()
      val debugInfo =
        s"""
           |*********************************
           |* Way point based Visit Options *
           |*********************************
           |
           |ROUTING OPTIONS
           | FROM   : ${locationStrToRouteVia.head}
           | TO     : ${locationStrToRouteVia.last}
           | FREIGHT: $followFreightLinks
           | FIXED  : $followFixedLinks
           | UNKNOWN: $followUnknownLinks
           |
           |QUICK VISIT OPTIONS
           | VISIT MODE         : $visitMode
           | FROM LOCATION INDEX: $fromLocationIndex
           | FROM LOCATION ID   : ${locationStrToRouteVia(fromLocationIndex)}
           | TO LOCATION INDEX  : $toLocationIndex
           | TO LOCATION ID     : ${locationStrToRouteVia(toLocationIndex)}
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


      var lastVisitTime = visitStartTime

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
          val debugVisitLocation = s"VISIT LOCATION: Visiting Index $index, lastVisitTime $lastVisitTime, location ${nextLocation.id}, travelTime $travelTime"
          debugStrBuf.append(debugVisitLocation).append("\n")
          val visitDescription = ""
          locationsService.visitLocation(nextLocation, lastVisitTime, lastVisitTime, visitDescription , request.user)

          lastVisitTime = lastVisitTime.plusSeconds(travelTime)
        }

      })

      val mapLocationList = path.locations.map(MapLocation(_))
      val locationsToRouteVia = path.locations.map(_.id)

      val mapRouteList = path.routes.map(r => MapRoute(r))
      val routeList = path.routes
      val waypoints = path.locations.map(l => Waypoint(l.id, l.name, l.isOrrStation))

      val distance = path.routes.map(_.distance).sum
      val time = path.routes.map(_.travelTimeInSeconds).map(_.getSeconds).sum


      val messages = if(debug) debugStrBuf.toString.split("\n").toList else List.empty

      play.api.mvc.Results.Ok(views.html.plan.route.find.pointtopoint.result.index(
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
          mkTime(time, " (assuming no stops)"),
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
          startDate.getOrElse(""),
          startTime.getOrElse(""),
          setVisitDetails,
          visitName.getOrElse(""),
          trainUid.getOrElse(""),
          trainHeadcode.getOrElse(""),
          locationsToVisit,
          routesToVisit,
          controllers.plan.route.find.result.pointtopoint.visit.routes.FindPointToPointRouteResultVisitController.visit(),
          controllers.plan.route.find.pointtopoint.routes.PointToPointRouteController.index(locationsToRouteVia.mkString("\n"), path.followFixedLinks, path.followFreightLinks, path.followUnknownLinks)
        ),
        messages))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  private def makeVisitStartDateAndTime(overrideStartTime: Boolean, startDate: Option[String], startTime: Option[String]) = {
    if (overrideStartTime) {
      val startDateStr = startDate.getOrElse(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
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
      LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    } else LocalDateTime.now()
  }

  private def calculateRoutesAndLocationsToVisit(
                                          visitMode: Option[String],
                                          path: Path,
                                          fromLocationIndex: Int,
                                          toLocationIndex: Int,
                                          includeNonPublicStops: Boolean): (List[Location], List[Route]) = {
    val pathLocations = path.locations
    val pathRoutes = path.routes

    visitMode match {
      case Some("visitAllRoutesAndPublicStopsBetweenLocations") =>

        val splicedLocations = pathLocations.slice(fromLocationIndex, toLocationIndex + 1)
        val setOfLocationsToVisit = if (!includeNonPublicStops) splicedLocations.filter(_.isOrrStation) else splicedLocations

        val routeStartIndex = fromLocationIndex
        val routeEndIndex = toLocationIndex - 1
        val splicedRoutes = pathRoutes.slice(routeStartIndex, routeEndIndex + 1)
        val setOfRoutesToVisit = splicedRoutes

        (setOfLocationsToVisit, setOfRoutesToVisit)

      case Some("visitAllRoutesAndPublicStops") =>
        val setOfRoutesToVisit = pathRoutes
        val setOfLocationsToVisit = if (!includeNonPublicStops) pathLocations.filter(_.isOrrStation) else pathLocations

        (setOfLocationsToVisit, setOfRoutesToVisit)

      case Some("visitAllRoutes") =>
        val setOfRoutesToVisit = pathRoutes
        val setOfLocationsToVisit = List.empty

        (setOfLocationsToVisit, setOfRoutesToVisit)

      case Some("visitAllPublicStops") =>
        val setOfLocationsToVisit = if (!includeNonPublicStops) pathLocations.filter(_.isOrrStation) else pathLocations
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

