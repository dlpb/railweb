package controllers.admin.users.delete

import java.util.Date

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.AuthorizedWebAction
import controllers.admin.AdminHelpers
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.auth.roles.AdminUser
import play.api.mvc._
import services.visit.route.RouteVisitService

@Singleton
class DeleteUserController @Inject()(
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
        Ok(views.html.admin.users.delete.index(token, request.user, getAndSortUsers, List()))
      }
  }


  def deleteUser = authorizedAction {
    implicit request =>
      if (!request.user.roles.contains(AdminUser)) Unauthorized("Unauthorized")
      else {

        val data = request.request.body.asFormUrlEncoded
        val token = jwtService.createToken(request.user, new Date())

        def view(messages: List[String]): Result = Ok(views.html.admin.users.delete.index(token, request.user, getAndSortUsers, messages))

        def fn(data: Option[Map[String, Seq[String]]]): Result = {
          data.get("id").headOption match {
            case Some(id) =>
              userDao.findUserById(id.toLong) flatMap { u => userDao.getDaoUser(u) } match {
                case Some(daoUser) =>
                  userDao.deleteUser(daoUser)
                  Ok(views.html.admin.users.delete.index(token, request.user, getAndSortUsers, List(s"Deleted user")))
                case None =>
                  Ok(views.html.admin.users.delete.index(token, request.user, getAndSortUsers, List("No user data")))
              }

            case _ =>
              Ok(views.html.admin.users.delete.index(token, request.user, getAndSortUsers, List("Invalid data")))
          }
        }

        AdminHelpers.ensureValidConfirmation(userDao, request, data, view _, fn _)
      }
  }

  private def getAndSortUsers = {
    userDao.getUsers.toList.sortBy(_.id)
  }
}

