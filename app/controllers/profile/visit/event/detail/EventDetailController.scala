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
import services.visit.event.EventService
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

@Singleton
class EventDetailController @Inject()(
                                       userDao: UserDao,
                                       jwtService: JWTService,
                                       cc: ControllerComponents,
                                       locationsService: LocationService,
                                       locationVisitService: LocationVisitService,
                                       routeVisitService: RouteVisitService,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       eventService: EventService,
                                       authorizedAction: AuthorizedAction
) extends AbstractController(cc) {

  def index(id: String) = authenticatedUserAction {
    implicit request: WebUserContext[AnyContent] =>
      val eventOption = eventService.getEventFromId(id, request.user)
      eventOption
        .map({ event =>
          val visitedLocations = locationVisitService
            .getLocationsVisitedForEvent(event, request.user)

          val visitedMapLocations = visitedLocations
            .map({
              MapLocation(_)
            })
            .sortBy(_.name)

          val visitedRoutes: List[Route] =
            routeVisitService.getRoutesVisitedForEvent(event, request.user)

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
            .map(l => l.id -> false)
            .toMap

          val locationVisitIndex = visitedLocations
            .flatMap(l => locationsService.findFirstLocationByTiploc(l.id))
            .map(
              l =>
                l.id -> locationVisitService
                  .getStationVisitNumber(request.user, l.id)
            )
            .toMap

          Ok(
            views.html.visits.event.detail.index(
              request.user,
              visitedMapRoutes,
              visitedMapLocations,
              firstVisits,
              locationVisitIndex,
              event.name,
              distance
            )
          )
        })
        .getOrElse(
          NotFound(
            views.html.visits.event.detail.index(
              request.user,
              List.empty,
              List.empty,
              Map.empty,
              Map.empty,
              s"$id not found",
              0
            )
          )
        )

  }

}
