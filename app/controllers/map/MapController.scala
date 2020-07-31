package controllers.map

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import controllers.routes
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
  def showMapPage(colour: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
      val token = jwtService.createToken(request.user, new Date())
      Ok(views.html.map.index(request.user, token, controllers.api.authenticated.routes.ApiAuthenticatedController.visitLocation(), colour)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }


  }

}
