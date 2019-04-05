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

  def admin = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.admin(token, request.user))
      }
  }

  def adminUsers = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.adminUsers(token, request.user))
      }
  }

  def adminUsersCreateView = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.adminUsersCreate(token, request.user, userDao.getUsers, List()))
      }
  }

  def adminUsersUpdateView = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.adminUsersUpdate(token, request.user, userDao.getUsers, List()))
      }
  }

  def adminUsersDeleteView = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.adminUsersDelete(token, request.user, userDao.getUsers, List()))
      }
  }

  def adminUpdateUserDataView(userId: Long): Action[AnyContent] = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        userDao.findUserById(userId) match {
          case Some(user) =>
            val locations = locationsService.getVisitsForUser(user)
            val routes = routesService.getVisitsForUser(user)
            Ok(views.html.adminUserUpdateData(token, request.user, user.id, VisitJsonUtils.toJson(locations), VisitJsonUtils.toJson(routes), List()))
          case None =>
            NotFound(views.html.adminUserUpdateData(token, request.user, -1L, "", "", List(s"User with id $userId not found")))
        }
      }
  }

  def adminUpdateUserData(userId: Long): Action[AnyContent] = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {

        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        (data.get("locations").headOption, data.get("routes").headOption) match {
          case (Some(locations), Some(routes)) =>
            data.get("confirmation").headOption match {
              case Some(confirmation) =>
                userDao.getDaoUser(request.user) match {
                  case Some(adminUser) =>
                    val encryptedConfirmation = userDao.encryptPassword(confirmation)
                    if (encryptedConfirmation.equals(adminUser.password)) {
                      userDao.findUserById(userId) match {
                        case Some(user) =>
                          locationsService.saveVisits(VisitJsonUtils.fromJson(locations), user)
                          routesService.saveVisits(VisitJsonUtils.fromJson(routes), user)

                          Ok(views.html.adminUserUpdateData(token, request.user, userId, locations, routes, List("Saved")))
                        case None =>
                          Ok(views.html.adminUserUpdateData(token, request.user, userId, locations, routes, List("could not find user")))
                      }
                    }
                    else
                      Ok(views.html.adminUserUpdateData(token, request.user, userId, locations, routes, List("Invalid confirmation")))
                  case None =>
                    Ok(views.html.adminUserUpdateData(token, request.user, userId, locations, routes, List("Error finding admin user")))
                }
              case None =>
                Ok(views.html.adminUserUpdateData(token, request.user, userId, locations, routes, List("Please enter confirmation")))
            }
          case _ =>
            Ok(views.html.adminUserUpdateData(token, request.user, userId, "", "", List("Saved")))
        }
      }
  }

  def adminDeleteUser = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) Unauthorized("Unauthorized")
      else {

        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        data.get("confirmation").headOption match {
          case Some(confirmation) =>
            userDao.getDaoUser(request.user) match {
              case Some(adminUser) =>
                val encryptedConfirmation = userDao.encryptPassword(confirmation)
                if (encryptedConfirmation.equals(adminUser.password)) {
                  data.get("id").headOption match {
                    case Some(id) =>
                      userDao.findUserById(id.toLong) flatMap { u => userDao.getDaoUser(u) } match {
                        case Some(daoUser) =>
                          userDao.deleteUser(daoUser)
                          Ok(views.html.adminUsersDelete(token, request.user, userDao.getUsers, List(s"Deleted user")))
                        case None =>
                          Ok(views.html.adminUsersDelete(token, request.user, userDao.getUsers, List("No user data")))
                      }

                    case _ =>
                      Ok(views.html.adminUsersDelete(token, request.user, userDao.getUsers, List("Invalid data")))
                  }
                }
                else
                  Ok(views.html.adminUsersDelete(token, request.user, userDao.getUsers, List("Invalid confirmation")))
              case None =>
                Ok(views.html.adminUsersDelete(token, request.user, userDao.getUsers, List("Error finding admin user")))
            }
          case None =>
            Ok(views.html.adminUsersDelete(token, request.user, userDao.getUsers, List("Please enter confirmation")))
        }
      }
  }

  def adminUpdateUser = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) Unauthorized("Unauthorized")
      else {

        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        data.get("confirmation").headOption match {
          case Some(confirmation) =>
            userDao.getDaoUser(request.user) match {
              case Some(adminUser) =>
                val encryptedConfirmation = userDao.encryptPassword(confirmation)
                if (encryptedConfirmation.equals(adminUser.password)) {
                  (data.get("id").headOption, data.get("username").headOption, data.get("password").headOption, data.get("roles").headOption) match {
                    case (Some(id), Some(username), Some(password), Some(roles)) =>
                      userDao.findUserById(id.toLong) flatMap { u => userDao.getDaoUser(u) } match {
                        case Some(daoUser) =>
                          userDao.updateUser(daoUser.copy(username = username, password = userDao.encryptPassword(password), roles = roles.split(",").toSet))
                          Ok(views.html.adminUsersUpdate(token, request.user, userDao.getUsers, List(s"Updated user $id $username")))
                        case None =>
                          Ok(views.html.adminUsersUpdate(token, request.user, userDao.getUsers, List("No user data")))
                      }

                    case _ =>
                      Ok(views.html.adminUsersUpdate(token, request.user, userDao.getUsers, List("Invalid data")))
                  }
                }
                else
                  Ok(views.html.adminUsersUpdate(token, request.user, userDao.getUsers, List("Invalid confirmation")))
              case None =>
                Ok(views.html.adminUsersUpdate(token, request.user, userDao.getUsers, List("Error finding admin user")))
            }
          case None =>
            Ok(views.html.adminUsersUpdate(token, request.user, userDao.getUsers, List("Please enter confirmation")))
        }
      }
  }

  def adminCreateUser = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) Unauthorized("Unauthorized")
      else {
        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        data.get("confirmation").headOption match {
          case Some(confirmation) =>
            userDao.getDaoUser(request.user) match {
              case Some(adminUser) =>
                val encryptedConfirmation = userDao.encryptPassword(confirmation)
                if (encryptedConfirmation.equals(adminUser.password)) {
                  (data.get("username").headOption, data.get("password").headOption, data.get("roles").headOption) match {
                    case (Some(username), Some(password), Some(roles)) =>
                      if (userDao.usernameInUse(username))
                        Ok(views.html.adminUsersCreate(token, request.user, userDao.getUsers, List(s"Username $username is already in use")))
                      else {
                        userDao.createUser(username, password, roles.split(",").toSet)
                        Ok(views.html.adminUsersCreate(token, request.user, userDao.getUsers, List()))
                      }

                    case _ =>
                      Ok(views.html.adminUsersCreate(token, request.user, userDao.getUsers, List("Invalid data")))
                  }
                }
                else
                  Ok(views.html.adminUsersCreate(token, request.user, userDao.getUsers, List("Invalid confirmation")))
              case None =>
                Ok(views.html.adminUsersCreate(token, request.user, userDao.getUsers, List("Error finding admin user")))
            }
          case None =>
            Ok(views.html.adminUsersCreate(token, request.user, userDao.getUsers, List("Please enter confirmation")))
        }
      }
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
