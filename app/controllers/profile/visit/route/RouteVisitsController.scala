package controllers.profile.visit.route

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.postgres.RouteDataIdConverter
import models.location.{LocationsService, MapLocation}
import models.route.display.map.MapRoute
import models.route.Route
import models.visits.Event
import models.visits.route.RouteVisitService
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.route.RouteService

@Singleton
class RouteVisitsController @Inject()(
                                       userDao: UserDao,
                                       jwtService: JWTService,
                                       cc: ControllerComponents,
                                       locationsService: LocationsService,
                                       routesService: RouteService,
                                       routeVisitService: RouteVisitService,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       authorizedAction: AuthorizedAction
                                     ) extends AbstractController(cc) {

  def index = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val routes: Map[(String, String), List[String]] = routeVisitService.getVisitsForUser(request.user)
      .getOrElse(Map.empty[String, List[String]])
      .filter(_._2.nonEmpty)
      .map {
        r =>
          RouteDataIdConverter.stringToRouteIds(r._1) -> r._2
      }

    val invalidRoutes: Set[(String, String)] = routes.keySet.filter(r => routesService.findRoute(r._1, r._2).isEmpty)

    val mapRoutes: List[MapRoute] = routes
      .filter(_._2.nonEmpty)
      .keySet
      .flatMap { r => routesService.findRoute(r._1, r._2) }
      .map {
        MapRoute(_)
      }
      .toList
      .sortBy(r => r.from.id + " - " + r.to.id)

    Ok(views.html.visits.route.index(request.user, routes, invalidRoutes, mapRoutes))

  }
}

