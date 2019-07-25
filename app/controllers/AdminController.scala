package controllers

import java.util.Date

import auth.JWTService
import auth.api.{AuthorizedAction, UserRequest}
import auth.web.AuthorizedWebAction
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.auth.roles.AdminUser
import models.data.VisitJsonUtils
import models.location.LocationsService
import models.route.{Route, RoutePoint, RoutesService}
import play.api.mvc._

@Singleton
class AdminController @Inject()(
                                             userDao: UserDao,
                                             jwtService: JWTService,
                                             cc: ControllerComponents,
                                             locationsService: LocationsService,
                                             routesService: RoutesService,
                                             authenticatedUserAction: AuthorizedWebAction,
                                             authorizedAction: AuthorizedAction
                                           ) extends AbstractController(cc) {

  def admin = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.admin.index(token, request.user))
      }
  }

  def adminUsers = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.admin.users(token, request.user))
      }
  }

  def adminUsersCreateView = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.admin.usersCreate(token, request.user, userDao.getUsers, List()))
      }
  }

  def adminUsersUpdateView = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.admin.usersUpdate(token, request.user, userDao.getUsers, List()))
      }
  }

  def adminUsersDeleteView = authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.admin.usersDelete(token, request.user, userDao.getUsers, List()))
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
            Ok(views.html.admin.userUpdateData(token, request.user, user.id, VisitJsonUtils.toJson(locations), VisitJsonUtils.toJson(routes), List()))
          case None =>
            NotFound(views.html.admin.userUpdateData(token, request.user, -1L, "", "", List(s"User with id $userId not found")))
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

  def adminDeleteUser = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) Unauthorized("Unauthorized")
      else {

        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        def view(messages: List[String]): Result = Ok(views.html.admin.usersDelete(token, request.user, userDao.getUsers, messages))

        def fn(data: Option[Map[String, Seq[String]]]): Result = {
          data.get("id").headOption match {
            case Some(id) =>
              userDao.findUserById(id.toLong) flatMap { u => userDao.getDaoUser(u) } match {
                case Some(daoUser) =>
                  userDao.deleteUser(daoUser)
                  Ok(views.html.admin.usersDelete(token, request.user, userDao.getUsers, List(s"Deleted user")))
                case None =>
                  Ok(views.html.admin.usersDelete(token, request.user, userDao.getUsers, List("No user data")))
              }

            case _ =>
              Ok(views.html.admin.usersDelete(token, request.user, userDao.getUsers, List("Invalid data")))
          }
        }

        ensureValidConfirmation(request, data, view _, fn _)
      }
  }

  def adminUpdateUser = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) Unauthorized("Unauthorized")
      else {

        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        def view(messages: List[String]): Result = Ok(views.html.admin.usersUpdate(token, request.user, userDao.getUsers, messages))

        def fn(data: Option[Map[String, Seq[String]]]): Result = {
          (data.get("id").headOption, data.get("username").headOption, data.get("password").headOption, data.get("roles").headOption) match {
            case (Some(id), Some(username), Some(password), Some(roles)) =>
              userDao.findUserById(id.toLong) flatMap { u => userDao.getDaoUser(u) } match {
                case Some(daoUser) =>
                  userDao.updateUser(daoUser.copy(username = username, password = password, roles = roles.split(",").toSet))
                  Ok(views.html.admin.usersUpdate(token, request.user, userDao.getUsers, List(s"Updated user $id $username")))
                case None =>
                  Ok(views.html.admin.usersUpdate(token, request.user, userDao.getUsers, List("No user data")))
              }

            case _ =>
              Ok(views.html.admin.usersUpdate(token, request.user, userDao.getUsers, List("Invalid data")))
          }
        }
        ensureValidConfirmation(request, data, view _, fn _)
      }
  }

  def adminCreateUser = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) Unauthorized("Unauthorized")
      else {
        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        def view(messages: List[String]): Result = Ok(views.html.admin.usersCreate(token, request.user, userDao.getUsers, messages))

        def fn(data: Option[Map[String, Seq[String]]]): Result = {
          (data.get("username").headOption, data.get("password").headOption, data.get("roles").headOption) match {
            case (Some(username), Some(password), Some(roles)) =>
              if (userDao.usernameInUse(username))
                Ok(views.html.admin.usersCreate(token, request.user, userDao.getUsers, List(s"Username $username is already in use")))
              else {
                userDao.createUser(username, password, roles.split(",").toSet)
                Ok(views.html.admin.usersCreate(token, request.user, userDao.getUsers, List()))
              }

            case _ =>
              Ok(views.html.admin.usersCreate(token, request.user, userDao.getUsers, List("Invalid data")))
          }
        }
        ensureValidConfirmation(request, data, view _, fn _)
      }
  }

  def adminMigrateRouteView(from: String, to: String) =  authenticatedUserAction {
    implicit request =>
      val token = jwtService.createToken(request.user, new Date())
      if (!request.user.roles.contains(AdminUser)) {
        Redirect(routes.LandingPageController.showLandingPage())
      }
      else {
        Ok(views.html.admin.migrateRoute(token, request.user, from, to,"", List()))
      }
  }

  def adminMigrateRoute = authorizedAction {
    implicit request =>
      def makeDummyRoute(from: String, to: String) = Route(RoutePoint(0.0, 0.0, from, "", ""), RoutePoint(0.0, 0.0, to, "", ""), "", "", "", "", "", "")
      if (!request.user.roles.contains(AdminUser)) Unauthorized("Unauthorized")
      else {
        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        def view(messages: List[String]): Result = Ok(views.html.admin.migrateRoute(token, request.user, "", "","", messages))

        def fn(data: Option[Map[String, Seq[String]]]): Result = {
          (data.get("from").headOption, data.get("to").headOption, data.get("newRoutes").headOption) match {
            case (Some(from), Some(to), Some(newRoutes)) =>
              val oldRoute = makeDummyRoute(from, to)
              val routes: List[Route] = newRoutes.split("\n").toList flatMap {
                inputRoute =>
                  println(inputRoute)
                  if(!inputRoute.contains(",")) Ok(views.html.admin.migrateRoute(token, request.user, from, to, newRoutes, List(s"Error processing $inputRoute. Must contain a comma separated value on a line")))
                  val routeParts = inputRoute.split(",")
                  if(!routeParts.size.equals(2)) Ok(views.html.admin.migrateRoute(token, request.user, from, to, newRoutes, List(s"Error processing $inputRoute. Must contain a comma separated value on a line")))
                  val route = routesService.getRoute(routeParts(0).trim, routeParts(1).trim)
                  if(route.isEmpty) Ok(views.html.admin.migrateRoute(token, request.user, from, to, newRoutes, List(s"Error processing $inputRoute. No valid route found")))
                  route
              }
              userDao.getUsers foreach { user =>
                userDao.findUserById(user.id) foreach {
                  routesService.migrate(oldRoute, routes, _)
                }
              }
              Ok(views.html.admin.migrateRoute(token, request.user, from, to, newRoutes, List("Updated")))

            case _ =>
              Ok(views.html.admin.migrateRoute(token, request.user,"", "","", List("Invalid data")))
          }
        }
        ensureValidConfirmation(request, data, view _, fn _)
      }
  }

  private def ensureValidConfirmation(request: UserRequest[AnyContent],
                                      data: Option[Map[String, Seq[String]]],
                                      view: List[String] => Result,
                                      fn: Option[Map[String, Seq[String]]] => Result) = {
    data.get("confirmation").headOption match {
      case Some(confirmation) =>
        userDao.getDaoUser(request.user) match {
          case Some(adminUser) =>
            val encryptedConfirmation = userDao.encryptPassword(confirmation)
            if (encryptedConfirmation.equals(adminUser.password)) {
              fn(data)
            }
            else {
              view(List("Invalid confirmation"))
            }
          case None =>
            view(List("Error finding admin user"))
        }
      case None =>
        view(List("Please enter confirmation"))
    }
  }
}

