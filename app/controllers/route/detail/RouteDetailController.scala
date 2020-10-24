package controllers.route.detail

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import services.visit.route.RouteVisitService
import play.api.mvc._
import services.route.RouteService


@Singleton
class RouteDetailController @Inject()(
                                       cc: ControllerComponents,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       routeService: RouteService,
                                       routeVisitService: RouteVisitService,
                                       jwtService: JWTService

) extends AbstractController(cc) {

  def index(from: String, to: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
   if(request.user.roles.contains(MapUser)){
     val route = routeService.findRoute(from, to)
     route match {
       case Some(r) =>
         val token = jwtService.createToken(request.user, new Date())
         Ok(views.html.route.detail.index(
           request.user,
           r,
           routeVisitService.getVisitsForRoute(r, request.user),
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
