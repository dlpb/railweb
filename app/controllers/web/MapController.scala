package controllers.web

import java.util.Date

import auth.api.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import play.api.mvc._


@Singleton
class MapController @Inject()(
                                       cc: ControllerComponents,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       jwtService: JWTService

                                     ) extends AbstractController(cc) {
  private val logoutUrl = routes.AuthenticatedUserController.logout

  def showMapPage() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
      val token = jwtService.createToken(request.user, new Date())
      Ok(views.html.map(token))
    }
    else {
      Forbidden("User not authorized to view page")
    }


  }

}
