package controllers.admin.users.update.data

import java.util.Date

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.AuthorizedWebAction
import javax.inject.Inject
import models.auth.UserDao
import models.auth.roles.AdminUser
import models.data.VisitJsonUtils
import models.location.LocationsService
import models.route.RoutesService
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class UpdateUserDataController @Inject()(
                                          userDao: UserDao,
                                          jwtService: JWTService,
                                          cc: ControllerComponents,
                                          locationsService: LocationsService,
                                          routesService: RoutesService,
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
            val locations = locationsService.getVisitsForUser(user)
            val routes = routesService.getVisitsForUser(user)
            Ok(views.html.admin.userUpdateData(token, request.user, user.id, VisitJsonUtils.toJson(locations), VisitJsonUtils.toJson(routes), List()))
          case None =>
            NotFound(views.html.admin.userUpdateData(token, request.user, -1L, "", "", List(s"User with id $userId not found")))
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

                          VisitJsonUtils.fromJson(locations) match {
                            case Right(json) =>
                              locationsService.saveVisits(json, user)
                              messages = "Saved Locations" :: messages
                            case Left(msg) =>
                              messages = msg :: messages
                          }

                          VisitJsonUtils.fromJson(routes) match {
                            case Right(json) =>
                              routesService.saveVisits(json, user)
                              messages = "Saved Routes" :: messages
                            case Left(msg) =>
                              messages = msg :: messages

                          }
                          Ok(views.html.admin.userUpdateData(token, request.user, userId, locations, routes, messages))
                        case None =>
                          Ok(views.html.admin.userUpdateData(token, request.user, userId, locations, routes, List("could not find user")))
                      }
                    }
                    else
                      Ok(views.html.admin.userUpdateData(token, request.user, userId, locations, routes, List("Invalid confirmation")))
                  case None =>
                    Ok(views.html.admin.userUpdateData(token, request.user, userId, locations, routes, List("Error finding admin user")))
                }
              case None =>
                Ok(views.html.admin.userUpdateData(token, request.user, userId, locations, routes, List("Please enter confirmation")))
            }
          case _ =>
            Ok(views.html.admin.userUpdateData(token, request.user, userId, "", "", List("Saved")))
        }
      }
  }
}
