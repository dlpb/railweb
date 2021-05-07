package controllers.admin.users.update.data

import java.util.Date

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.AuthorizedWebAction
import javax.inject.Inject
import models.auth.UserDao
import models.auth.roles.AdminUser
import models.location.Location
import models.route.Route
import services.visit.route.RouteVisitService
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.visit.location.LocationVisitService

class UpdateUserDataController @Inject()(
                                          userDao: UserDao,
                                          jwtService: JWTService,
                                          cc: ControllerComponents,
                                          locationVisitService: LocationVisitService,
                                          routeVisitService: RouteVisitService,
                                          authenticatedUserAction: AuthorizedWebAction,
                                          authorizedAction: AuthorizedAction
                                        ) extends AbstractController(cc) {

  def index(userId: Long): Action[AnyContent] = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(controllers.landing.routes.LandingPageController.showLandingPage())
      }
      else {
        userDao.findUserById(userId) match {
          case Some(user) =>
            Ok(views.html.admin.users.update.data.index(
              token,
              request.user,
              user.id,
              locationVisitService.getVisitsAsJson(user),
              routeVisitService.getVisitsAsJson(user),
              List()))
          case None =>
            NotFound(views.html.admin.users.update.data.index(token, request.user, -1L, "", "", List(s"User with id $userId not found")))
        }
      }
  }


  def updateUserData(userId: Long): Action[AnyContent] = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(controllers.landing.routes.LandingPageController.showLandingPage())
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
                          var messages: List[String] = List()

                          locationVisitService.saveVisitsAsJson(locations, user)
                          routeVisitService.saveVisitsAsJson(routes, user)

                          Ok(views.html.admin.users.update.data.index(token, request.user, userId, locations, routes, messages))
                        case None =>
                          Ok(views.html.admin.users.update.data.index(token, request.user, userId, locations, routes, List("could not find user")))
                      }
                    }
                    else
                      Ok(views.html.admin.users.update.data.index(token, request.user, userId, locations, routes, List("Invalid confirmation")))
                  case None =>
                    Ok(views.html.admin.users.update.data.index(token, request.user, userId, locations, routes, List("Error finding admin user")))
                }
              case None =>
                Ok(views.html.admin.users.update.data.index(token, request.user, userId, locations, routes, List("Please enter confirmation")))
            }
          case _ =>
            Ok(views.html.admin.users.update.data.index(token, request.user, userId, "", "", List("Saved")))
        }
      }
  }
}
