package controllers.plan.route.find.result.visit

import java.net.URLDecoder
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
import play.api.mvc.Call
import services.visit.event.EventService
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

@Singleton
class FindRouteResultVisitController @Inject()(
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

      println(s"Building results for ${locationStrToRouteVia.head} to ${locationStrToRouteVia.last} Options: follow freight: $followFreightLinks, follow fixed: $followFixedLinks, follow unknown: $followUnknownLinks")

      val path: Path = pathService.findRouteForWaypoints(locationStrToRouteVia, followFixedLinks, followFreightLinks, followUnknownLinks)

      val overrideStartTime = FindRouteResultHelper.extractBooleanFromData(data, "overrideStartDateAndTime")
      val setVisitDetails = FindRouteResultHelper.extractBooleanFromData(data, "overrideVisitDetails")
      val includeNonPublicStops = FindRouteResultHelper.extractBooleanFromData(data, "includeNonPublicStopsInVisit")
      val visitMode = FindRouteResultHelper.extractString(data, "visitMode")
      val fromLocationIndex: Int = FindRouteResultHelper.extractInt(data, "from")
      val toLocationIndex: Int = FindRouteResultHelper.extractInt(data, "to")
      val startDate = FindRouteResultHelper.extractString(data, "startDate")
      val startTime = FindRouteResultHelper.extractString(data, "startTime")
      val visitName = FindRouteResultHelper.extractString(data, "visitName")
      val trainUid: Option[String] = FindRouteResultHelper.extractString(data, "trainUid")
      val trainHeadcode = FindRouteResultHelper.extractString(data, "trainHeadcode")


      println(s"" +
        s"Visit Options: " +
        s"overrideStartTime: $overrideStartTime, " +
        s"setVisitDetails: $setVisitDetails, " +
        s"includeNonPublicStops: $includeNonPublicStops, " +
        "visitMode: visitMode, " +
        s"fromLocationIndex: $fromLocationIndex, " +
        s"toLocationIndex: $toLocationIndex, " +
        "visitName: visitName, " +
        "trainUid: trainUid," +
        "trainHeadcode: trainHeadcode")


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




      var lastVisitTime = visitStartTime
      locationsToVisit.zipWithIndex.foreach(l => {
        val (nextLocation, index) = l
        println(s"Visiting Index $index, lastVisitTime $lastVisitTime, nextLocation ${nextLocation.id}")
        val nextRoute = if(index < routesToVisit.size) Some(routesToVisit(index)) else None
        val travelTime: Long = nextRoute.map(_.travelTimeInSeconds.getSeconds).getOrElse(0)
        val visitDescription =
          s"""{
                "visitType": "MANUAL_FROM_WAYPOINTS",
                "trainUid":${"trainUid"},
                "trainHeadcode":${"trainHeadcode"}
              }""".stripMargin
        locationsService.visitLocation(nextLocation, lastVisitTime, lastVisitTime, visitDescription , request.user)
        nextRoute.foreach(r => routesService.visitRoute(r, lastVisitTime, lastVisitTime, visitDescription, request.user))

        lastVisitTime = lastVisitTime.plusSeconds(travelTime)

      })

      val mapLocationList = path.locations.map(MapLocation(_))
      val locationsToRouteVia = path.locations.map(_.id)

      val mapRouteList = path.routes.map(r => MapRoute(r))
      val routeList = path.routes
      val waypoints = path.locations.map(l => Waypoint(l.id, l.name, l.isOrrStation))

      val distance = path.routes.map(_.distance).sum
      val time = path.routes.map(_.travelTimeInSeconds).map(_.getSeconds).sum


      val messages = List()

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
          toLocationIndex,
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
          controllers.plan.route.find.result.visit.routes.FindRouteResultVisitController.visit(),
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

