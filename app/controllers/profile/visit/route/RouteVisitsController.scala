package controllers.profile.visit.route

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.{Event, RouteVisit, Visit}
import models.route.Route
import models.route.display.map.MapRoute
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.route.RouteService
import services.visit.route.RouteVisitService

@Singleton
class RouteVisitsController @Inject()(
                                       userDao: UserDao,
                                       jwtService: JWTService,
                                       cc: ControllerComponents,
                                       routesService: RouteService,
                                       routeVisitService: RouteVisitService,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       authorizedAction: AuthorizedAction
                                     ) extends AbstractController(cc) {

  def index = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val routes: List[RouteVisit] = routeVisitService.getVisitsForUser(request.user)

    val routeVisitEvents: Map[(String, String), List[Event]] = routes.map(visit => {
      val events = routeVisitService.getEventsRouteWasVisited(visit.visited, request.user).distinctBy(_.id)
      (visit.visited.from.id, visit.visited.to.id) -> events
    }).toMap

    val mapRoutes: List[MapRoute] = routes
      .map { r => r.visited }
      .map {
        MapRoute(_)
      }
      .sortBy(r => r.from.id + " - " + r.to.id)

    Ok(views.html.visits.route.index(request.user, routes, routeVisitEvents, mapRoutes))

  }
}

