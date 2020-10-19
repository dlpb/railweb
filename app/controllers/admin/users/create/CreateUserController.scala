package controllers.admin.users.create

import java.util.Date

import auth.JWTService
import auth.api.{AuthorizedAction, UserRequest}
import auth.web.AuthorizedWebAction
import controllers.admin.AdminHelpers
import javax.inject.Inject
import models.auth.UserDao
import models.auth.roles.AdminUser
import models.location.LocationsService
import models.visits.route.RouteVisitService
import play.api.mvc.{AbstractController, ControllerComponents, Result}

class CreateUserController @Inject()(
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
        Ok(views.html.admin.users.create.index(token, request.user, userDao.getUsers, List()))
      }
  }

  def createUser = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) Unauthorized("Unauthorized")
      else {
        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        def view(messages: List[String]): Result = Ok(views.html.admin.users.create.index(token, request.user, userDao.getUsers, messages))

        def fn(data: Option[Map[String, Seq[String]]]): Result = {
          (data.get("username").headOption, data.get("password").headOption, data.get("roles").headOption) match {
            case (Some(username), Some(password), Some(roles)) =>
              if (userDao.usernameInUse(username))
                Ok(views.html.admin.users.create.index(token, request.user, userDao.getUsers, List(s"Username $username is already in use")))
              else {
                userDao.createUser(username, password, roles.split(",").toSet)
                Ok(views.html.admin.users.create.index(token, request.user, userDao.getUsers, List()))
              }

            case _ =>
              Ok(views.html.admin.users.create.index(token, request.user, userDao.getUsers, List("Invalid data")))
          }
        }
        AdminHelpers.ensureValidConfirmation(userDao, request, data, view _, fn _)
      }
  }

}
