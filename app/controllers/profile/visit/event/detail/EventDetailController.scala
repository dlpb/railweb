package controllers.profile.visit.event.detail

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.location.MapLocation
import models.route.Route
import models.route.display.map.MapRoute
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.location.LocationService
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

@Singleton
class EventDetailController @Inject()(
                                       userDao: UserDao,
                                       jwtService: JWTService,
                                       cc: ControllerComponents,
                                       locationsService: LocationService,
                                       locationVisitService: LocationVisitService,
                                       routesService: RouteVisitService,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       authorizedAction: AuthorizedAction
                                     ) extends AbstractController(cc) {

    def index(event: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val visitedRoutes: List[Route] = routesService.getRoutesVisitedForEvent(event, request.user)
    val visitedLocations = locationVisitService
      .getLocationsVisitedForEvent(event, request.user)
      .map({
        MapLocation(_)
      })
      .sortBy(_.name)

    val distance: Long = visitedRoutes
      .map({
        _.distance
      })
      .sum

    val visitedMapRoutes = visitedRoutes
      .map {
        MapRoute(_)
      }
      .sortBy(r => r.from.id + " - " + r.to.id)

      val firstVisits: Map[String, Boolean] = visitedLocations
          .map(l => l.id -> locationVisitService.isVisitFirstVisitForLocation(event, request.user, l.id))
          .toMap

      val locationVisitIndex = visitedLocations
          .flatMap(l => locationsService.findFirstLocationByTiploc(l.id))
          .map(l => l.id -> locationVisitService.getStationVisitNumber(request.user, l.id))
          .toMap

    Ok(views.html.visits.event.detail.index(request.user, visitedMapRoutes, visitedLocations, firstVisits, locationVisitIndex, event, distance))

  }

}

