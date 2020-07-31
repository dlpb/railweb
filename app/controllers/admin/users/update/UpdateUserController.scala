package controllers.admin.users.update

import java.util.Date

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.AuthorizedWebAction
import controllers.admin.AdminHelpers
import javax.inject.Inject
import models.auth.UserDao
import models.auth.roles.AdminUser
import models.location.LocationsService
import models.route.RoutesService
import play.api.mvc._

class UpdateUserController @Inject()(
                                      userDao: UserDao,
                                      jwtService: JWTService,
                                      cc: ControllerComponents,
                                      locationsService: LocationsService,
                                      routesService: RoutesService,
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
        Ok(views.html.admin.usersUpdate(token, request.user, getAndSortUsers, List()))
      }
  }

  def updateUser = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) Unauthorized("Unauthorized")
      else {

        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        def view(messages: List[String]): Result = Ok(views.html.admin.usersUpdate(token, request.user, getAndSortUsers, messages))

        def fn(data: Option[Map[String, Seq[String]]]): Result = {
          (data.get("id").headOption, data.get("username").headOption, data.get("password").headOption, data.get("roles").headOption) match {
            case (Some(id), Some(username), Some(password), Some(roles)) =>
              userDao.findUserById(id.toLong) flatMap { u => userDao.getDaoUser(u) } match {
                case Some(daoUser) =>
                  userDao.updateUser(daoUser.copy(username = username, password = password, roles = roles.split(",").toSet))
                  Ok(views.html.admin.usersUpdate(token, request.user, getAndSortUsers, List(s"Updated user $id $username")))
                case None =>
                  Ok(views.html.admin.usersUpdate(token, request.user, getAndSortUsers, List("No user data")))
              }

            case _ =>
              Ok(views.html.admin.usersUpdate(token, request.user, getAndSortUsers, List("Invalid data")))
          }
        }
        AdminHelpers.ensureValidConfirmation(userDao, request, data, view _, fn _)
      }
  }

  private def getAndSortUsers = {
    userDao.getUsers.toList.sortBy(_.id)
  }

}
