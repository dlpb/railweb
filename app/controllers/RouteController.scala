package controllers

import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.route.RoutesService
import play.api.mvc._


@Singleton
class RouteController @Inject()(
                                       cc: ControllerComponents,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       routeService: RoutesService

                                     ) extends AbstractController(cc) {
  private val logoutUrl = routes.AuthenticatedUserController.logout

  def showRouteDetailPage(from: String, to: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
   if(request.user.roles.contains(MapUser)){
     val route = routeService.getRoute(from, to)
     route match {
       case Some(r) => Ok(views.html.route(r))
       case None => NotFound("Route combination not found.")

     }
   }
   else {
     Forbidden("User not authorized to view page")
   }


  }

  def showRouteListPage() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
      val routes = routeService.defaultListRoutes
      Ok(views.html.routeList(routes))
    }
    else {
      Forbidden("User not authorized to view page")
    }


  }

}
