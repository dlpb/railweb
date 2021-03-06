package controllers

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import play.api.i18n.I18nSupport
import play.api.mvc._


@Singleton
class MapController @Inject()(
                                       cc: ControllerComponents,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       jwtService: JWTService

                                     ) extends AbstractController(cc) with I18nSupport {
  private val logoutUrl = routes.AuthenticatedUserController.logout

  def showMapPage() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
      val token = jwtService.createToken(request.user, new Date())
      Ok(views.html.map(request.user, token, routes.ApiAuthenticatedController.visitLocation)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }


  }

}
