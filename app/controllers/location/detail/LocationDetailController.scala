package controllers.location.detail

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.{GroupedListLocation, ListLocation, Location, LocationsService}
import play.api.mvc._


@Singleton
class LocationDetailController @Inject()(
                                          cc: ControllerComponents,
                                          authenticatedUserAction: AuthorizedWebAction,
                                          locationService: LocationsService,
                                          jwtService: JWTService

                                        ) extends AbstractController(cc) {

  def index(id: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {
      val location: Option[Location] = locationService.getLocation(id)
      val token = jwtService.createToken(request.user, new Date())
      location match {
        case Some(loc) => Ok(views.html.locations.location(
          request.user,
          loc,
          locationService.getVisitsForLocation(loc, request.user),
          token,
          controllers.api.authenticated.routes.ApiAuthenticatedController.visitLocationWithParams(id),
          controllers.api.authenticated.routes.ApiAuthenticatedController.removeLastVisitForLocation(id),
          controllers.api.authenticated.routes.ApiAuthenticatedController.removeAllVisitsForLocation(id)
        )(request.request))
        case None => NotFound("Location not found.")
      }
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
