package controllers.plan.route

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.list.PathService
import models.location.{LocationsService, MapLocation}
import models.route.{MapRoute, RoutesService}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

@Singleton
class ListController @Inject()(
                                cc: ControllerComponents,
                                authenticatedUserAction: AuthorizedWebAction,
                                pathService: PathService,
                                locationsService: LocationsService,
                                routesService: RoutesService,
                                jwtService: JWTService

                              ) extends AbstractController(cc) {

  def showListPage(waypoints: String, followFreightLinks: Boolean, followFixedLinks: Boolean, visitAllRoutes: Boolean, visitAllStations: Boolean) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {
      val locationsToRouteVia = waypoints.split("\n").toList

      val token = jwtService.createToken(request.user, new Date())

      try {
        val path = pathService.findRouteForWaypoints(locationsToRouteVia, followFixedLinks, followFreightLinks)
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

        Ok(views.html.path.index(request.user, token, mapLocations, mapRoutes, waypoints, followFreightLinks, followFixedLinks, distance, messages, visitAllRoutes, visitAllStations))
      }
      catch {
        case iae: IllegalArgumentException =>
          Ok(views.html.path.index(request.user, token, List.empty, List.empty, waypoints, followFreightLinks, followFixedLinks, 0, List(iae.getMessage), visitAllRoutes, visitAllStations))
      }

    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}