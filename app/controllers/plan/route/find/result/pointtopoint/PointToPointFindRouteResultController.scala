package controllers.plan.route.find.result.pointtopoint

import java.net.URLDecoder
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime}
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import controllers.plan.route.find.result.FindRouteResultHelper.mkTime
import controllers.plan.route.find.result.{FindRouteResultHelper, ResultViewModel, Waypoint}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.{Location, MapLocation}
import models.route.Route
import models.route.display.map.MapRoute
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.plan.pointtopoint.{Path, PointToPointRouteFinderService}
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

@Singleton
class PointToPointFindRouteResultController @Inject()(
                                     cc: ControllerComponents,
                                     authenticatedUserAction: AuthorizedWebAction,
                                     pathService: PointToPointRouteFinderService,
                                     locationsService: LocationVisitService,
                                     routesService: RouteVisitService,
                                     jwtService: JWTService

                                   ) extends AbstractController(cc) {

  def pointtopoint() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {

      val visitMode: String = "visitAllRoutesAndPublicStops"
      val fromLocationIndex: Int = 0
      val toLocationIndex: Int = 0
      val includeNonPublicStops: Boolean = false
      val overrideDateAndTimeOfVisit: Boolean = false
      val overriddenStartDate: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)
      val overriddenStartTime: String = LocalTime.now.format(DateTimeFormatter.ISO_TIME)
      val overrideVisitDetails: Boolean = false
      val overriddenVisitName: String = ""
      val overriddenTrainUid: String = ""
      val overriddenTrainHeadcode: String = ""
      val locationsToVisit: List[Location] = List.empty
      val routesToVisit: List[Route] = List.empty

      val token = jwtService.createToken(request.user, new Date())

      val data = request.request.body.asFormUrlEncoded

      val locationStrToRouteVia: List[String] = FindRouteResultHelper.extractStringListFromData(data, "waypoints")
      val followFixedLinks: Boolean = FindRouteResultHelper.extractBooleanFromData(data, "followFixedLinks")
      val followFreightLinks: Boolean = FindRouteResultHelper.extractBooleanFromData(data, "followFreightLinks")
      val followUnknownLinks: Boolean = FindRouteResultHelper.extractBooleanFromData(data, "followUnknownLinks")

      println(s"Building results for ${locationStrToRouteVia.head} to ${locationStrToRouteVia.last} Options: follow freight: $followFreightLinks, follow fixed: $followFixedLinks, follow unknown: $followUnknownLinks")

      val path: Path = pathService.findRouteForWaypoints(locationStrToRouteVia, followFixedLinks, followFreightLinks, followUnknownLinks)


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
          visitMode,
          fromLocationIndex,
          toLocationIndex,
          includeNonPublicStops,
          overrideDateAndTimeOfVisit,
          overriddenStartDate,
          overriddenStartTime,
          overrideVisitDetails,
          overriddenVisitName,
          overriddenTrainUid,
          overriddenTrainHeadcode,
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
}
