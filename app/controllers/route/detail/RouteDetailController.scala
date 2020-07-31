package controllers.route.detail

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.route.{ListRoute, RoutesService}
import play.api.mvc._

import scala.collection.immutable.ListMap


@Singleton
class RouteDetailController @Inject()(
                                       cc: ControllerComponents,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       routeService: RoutesService,
                                       jwtService: JWTService

) extends AbstractController(cc) {

  def index(from: String, to: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
   if(request.user.roles.contains(MapUser)){
     val route = routeService.getRoute(from, to)
     route match {
       case Some(r) =>
         val token = jwtService.createToken(request.user, new Date())
         Ok(views.html.route.route(
           request.user,
           r,
           routeService.getVisitsForRoute(r, request.user),
           token,
           controllers.api.route.visit.routes.VisitRouteApiController.visitRouteWithParams(from, to),
           controllers.api.route.visit.routes.VisitRouteApiController.removeLastVisitForRoute(from, to),
           controllers.api.route.visit.routes.VisitRouteApiController.removeAllVisitsForRoute(from, to)
         )(request.request))
       case None => NotFound("Route combination not found.")
     }
   }
   else {
     Forbidden("User not authorized to view page")
   }
  }
}
