package controllers.admin

import java.util.Date

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.AuthorizedWebAction
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.auth.roles.AdminUser
import play.api.mvc._
import services.visit.route.RouteVisitService

@Singleton
class AdminController @Inject()(
                                 userDao: UserDao,
                                 jwtService: JWTService,
                                 cc: ControllerComponents,
                                 routesService: RouteVisitService,
                                 authenticatedUserAction: AuthorizedWebAction,
                                 authorizedAction: AuthorizedAction
                               ) extends AbstractController(cc) {

  def index = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(controllers.landing.routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.admin.index(token, request.user))
      }
  }

}

