package controllers.plan.route

import java.net.URLDecoder
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.{LocationsService, MapLocation}
import models.plan.route.pointtopoint.PointToPointRouteFinderService
import models.route.display.map.MapRoute
import models.route.RoutesService
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

@Singleton
class PointToPointRouteController @Inject()(
                                             cc: ControllerComponents,
                                             authenticatedUserAction: AuthorizedWebAction,
                                             pathService: PointToPointRouteFinderService,
                                             locationsService: LocationsService,
                                             routesService: RoutesService,
                                             jwtService: JWTService

                              ) extends AbstractController(cc) {

  def index(waypoints: String, followFreightLinks: Boolean, followFixedLinks: Boolean, followUnknownLinks: Boolean,  visitAllRoutes: Boolean, visitAllStations: Boolean) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {
      val decodedWaypoints = URLDecoder.decode(waypoints, "utf-8")
      val locationsToRouteVia = decodedWaypoints.split("\n").toList

      println(s"Fixed: $followFixedLinks, unknown: $followUnknownLinks, freight: $followFreightLinks")

      val token = jwtService.createToken(request.user, new Date())

      try {
        val path = pathService.findRouteForWaypoints(locationsToRouteVia, followFixedLinks, followFreightLinks, followUnknownLinks)
        val distance = path.routes
          .map {_.distance}
          .sum
        val mapRoutes: List[MapRoute] = path.routes map { MapRoute(_) }
        val mapLocations: List[MapLocation] = path.locations map { MapLocation(_) }

        val messages = List()

        if(visitAllRoutes) {
          path.routes foreach {
            route =>
              routesService.visitRoute(route, request.user)
          }
        }

        if(visitAllStations) {
          path.locations foreach {
            location =>
              if(location.orrStation)
                locationsService.visitLocation(location, request.user)
          }
        }

        Ok(views.html.plan.route.pointtopoint.find.index(request.user, token, mapLocations, mapRoutes, decodedWaypoints, followFreightLinks, followFixedLinks, followUnknownLinks, distance, messages, visitAllRoutes, visitAllStations))
      }
      catch {
        case iae: IllegalArgumentException =>
          Ok(views.html.plan.route.pointtopoint.find.index(request.user, token, List.empty, List.empty, decodedWaypoints, followFreightLinks, followFixedLinks, followUnknownLinks, 0, List(iae.getMessage), visitAllRoutes, visitAllStations))
      }

    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}