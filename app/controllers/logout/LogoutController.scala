package controllers.logout

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.AuthorizedWebAction
import javax.inject._
import models.auth.UserDao
import play.api.mvc._

@Singleton
class LogoutController@Inject()(
                                 userDao: UserDao,
                                 jwtService: JWTService,
                                 cc: ControllerComponents,
                                 authenticatedUserAction: AuthorizedWebAction,
                                 authorizedAction: AuthorizedAction
                                           ) extends AbstractController(cc) {


  def logout = authenticatedUserAction { implicit request: Request[AnyContent] =>
    // docs: “withNewSession ‘discards the whole (old) session’”
    Redirect(controllers.login.routes.LoginController.index())
      .flashing("info" -> "You are logged out.")
      .withNewSession
  }

}
