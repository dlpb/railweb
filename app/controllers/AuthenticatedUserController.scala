package controllers

import java.util.Date

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject._
import models.auth.UserDao
import models.auth.roles.AdminUser
import models.data.VisitJsonUtils
import models.location.LocationsService
import models.route.RoutesService
import models.web.forms.ChangePassword
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc._

@Singleton
class AuthenticatedUserController @Inject()(
                                             userDao: UserDao,
                                             jwtService: JWTService,
                                             cc: ControllerComponents,
                                             locationsService: LocationsService,
                                             routesService: RoutesService,
                                             authenticatedUserAction: AuthorizedWebAction,
                                             authorizedAction: AuthorizedAction
                                           ) extends AbstractController(cc) {

  val form: Form[ChangePassword] = Form(
    mapping(
      "oldPassword" -> nonEmptyText,
      "newPassword" -> nonEmptyText,
      "confirmPassword" -> nonEmptyText,
    )(ChangePassword.apply)(ChangePassword.unapply)
  )

  def logout = authenticatedUserAction { implicit request: Request[AnyContent] =>
    // docs: “withNewSession ‘discards the whole (old) session’”
    Redirect(routes.UserController.showLoginForm)
      .flashing("info" -> "You are logged out.")
      .withNewSession
  }

  def profile = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val token = jwtService.createToken(request.user, new Date())
    Ok(views.html.profile(token, request.user, form, List()))
  }

  def visits = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val routes = routesService.getVisitsForUser(request.user).getOrElse(Map.empty[String, List[String]])
    val locations = locationsService.getVisitsForUser(request.user).getOrElse(Map.empty[String, List[String]])
    Ok(views.html.visits(locations, routes))

  }

  def changePassword = authorizedAction { implicit request =>

    val token = jwtService.createToken(request.user, new Date())
    val data = request.request.body.asFormUrlEncoded
    (data.get("oldPassword").headOption, data.get("newPassword").headOption, data.get("confirmPassword").headOption) match {
      case (Some(oldPassword), Some(newPassword), Some(confirmPassword)) =>
        userDao.getDaoUser(request.user) match {
          case Some(daoUser) =>
            val encryptedOldPassword = userDao.encryptPassword(oldPassword)
            if (encryptedOldPassword.equals(daoUser.password)) {
              if (newPassword.equals(confirmPassword)) {
                val newDaoUser = daoUser.copy(password = userDao.encryptPassword(newPassword))
                userDao.updateUser(newDaoUser)
                Ok(views.html.profile(token, request.user, form, List()))
              } else
                Ok(views.html.profile(token, request.user, form, List("Passwords do not match")))
            }
            else
              Ok(views.html.profile(token, request.user, form, List("Password was not correct")))
          case None =>
            Ok(views.html.profile(token, request.user, form, List("Something went wrong, please try again. Error 2")))
        }
      case _ =>
        Ok(views.html.profile(token, request.user, form, List("Something went wrong, please try again. Error 1")))
    }
  }

}
