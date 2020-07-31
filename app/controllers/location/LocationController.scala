package controllers.location

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import controllers.routes
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.{GroupedListLocation, ListLocation, Location, LocationsService}
import play.api.mvc._


@Singleton
class LocationController @Inject()(
                                       cc: ControllerComponents,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       locationService: LocationsService,
                                       jwtService: JWTService

                                     ) extends AbstractController(cc) {



  def index() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
     Ok(views.html.locations.index(
       request.user,
        )(request.request))
      }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
