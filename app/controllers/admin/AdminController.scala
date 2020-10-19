package controllers.admin

import java.util.Date

import auth.JWTService
import auth.api.{AuthorizedAction, UserRequest}
import auth.web.AuthorizedWebAction
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.auth.roles.AdminUser
import models.location.{LocationsService, MapLocation}
import models.route.{Route, RoutePoint}
import models.visits.route.RouteVisitService
import org.json4s.DefaultFormats
import play.api.mvc._

@Singleton
class AdminController @Inject()(
                                 userDao: UserDao,
                                 jwtService: JWTService,
                                 cc: ControllerComponents,
                                 locationsService: LocationsService,
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

