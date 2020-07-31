package controllers.landing

import auth.web.AuthorizedWebAction
import controllers.routes
import javax.inject._
import play.api.mvc._

@Singleton
class LandingPageController @Inject()(
                                       cc: ControllerComponents,
                                       authenticatedUserAction: AuthorizedWebAction
                                     ) extends AbstractController(cc) {

  private val logoutUrl = controllers.logout.routes.LogoutController.logout()

  // this is where the user comes immediately after logging in.
  // notice that this uses `authenticatedUserAction`.
  def showLandingPage() = authenticatedUserAction { implicit request =>
    Ok(views.html.landing.index(request.user))
  }

}