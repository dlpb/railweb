package controllers.location.detail

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.Location
import play.api.mvc._
import services.location.{AdjacentLocationService, LocationService}
import services.visit.location.LocationVisitService


@Singleton
class LocationDetailController @Inject()(
                                          cc: ControllerComponents,
                                          authenticatedUserAction: AuthorizedWebAction,
                                          locationService: LocationService,
                                          adjacentLocationService: AdjacentLocationService,
                                          locationVisitService: LocationVisitService,
                                          jwtService: JWTService,
                                        ) extends AbstractController(cc) {

  def index(id: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {
      val location: Option[Location] = locationService.findFirstLocationByTiploc(id)
      val token = jwtService.createToken(request.user, new Date())

      location match {
        case Some(loc) =>
          val value = adjacentLocationService.findAdjacentLocations(loc)

          Ok(views.html.locations.detail.index(
          request.user,
          loc,
          locationVisitService.getVisitsForLocation(loc, request.user),
          token,
          controllers.api.locations.visit.routes.VisitLocationsApiController.visitLocationWithParams(id),
          controllers.api.locations.visit.routes.VisitLocationsApiController.removeLastVisitForLocation(id),
          controllers.api.locations.visit.routes.VisitLocationsApiController.removeAllVisitsForLocation(id),
          value
        )(request.request))
        case None => NotFound("Location not found.")
      }
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
