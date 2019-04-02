package controllers

import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject._
import play.api.mvc._

@Singleton
class AuthenticatedUserController @Inject()(
                                             cc: ControllerComponents,
                                             authenticatedUserAction: AuthorizedWebAction
                                           ) extends AbstractController(cc) {

  def logout = authenticatedUserAction { implicit request: Request[AnyContent] =>
    // docs: “withNewSession ‘discards the whole (old) session’”
    Redirect(routes.UserController.showLoginForm)
      .flashing("info" -> "You are logged out.")
      .withNewSession
  }

  def profile = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    Ok(views.html.profile(request.user))
  }

}
