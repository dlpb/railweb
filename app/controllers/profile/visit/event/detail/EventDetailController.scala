package controllers.profile.visit.event.detail

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.{Event, LocationVisit, RouteVisit}
import models.location.{Location, MapLocation}
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
              .sortBy(_.eventOccurredAt)
              .map({
                v => MapLocation(v.visited)
              })


          val visitedRoutes: List[RouteVisit] =
            routeVisitService
            .getRoutesVisitedForEvent(event, request.user)
            .sortBy(_.eventOccurredAt)

          val distance: Long = visitedRoutes
            .map({
              _.visited.distance
            })
            .sum

          val visitedMapRoutes = visitedRoutes
            .map {
              v => MapRoute(v.visited)
            }

          val allVisitedLocations = locationVisitService.getVisitsForUser(request.user)
          val allVisitedRoutes = routeVisitService.getVisitsForUser(request.user)

          val locationToVisits: Map[Location, List[LocationVisit]] = visitedLocations
            .map(locationVisit => {
              val location = locationVisit.visited
              val visitCount = allVisitedLocations.filter(_.visited.id.equals(location.id))
              location -> visitCount
            }).toMap

          val locationIdToVisitCount: Map[String, Int] =
            locationToVisits
              .iterator
              .map(l => l._1.id -> l._2.size)
              .toMap

          val routesToVisits: Map[Route, List[RouteVisit]] = visitedRoutes
            .map(routeVisit => {
              val route = routeVisit.visited
              val visitCount = allVisitedRoutes.filter(_.visited.equals(route))
              route -> visitCount
            }).toMap

          val routeToVisitCount: Map[MapRoute, Int] =
            routesToVisits
              .iterator
              .map(l => MapRoute(l._1) -> l._2.size)
              .toMap

          val locationFirstVisits: List[String] = allVisitedLocations
            .groupBy(_.visited)
            .iterator
            .map(v => v._1 -> v._2.minBy(_.eventOccurredAt))
            .map(_._2)
            .toList
            .filter(v => (v.eventOccurredAt.isEqual(event.startedAt) || v.eventOccurredAt.isAfter(event.startedAt) && (v.eventOccurredAt.isBefore(event.endedAt) || v.eventOccurredAt.isEqual(event.endedAt))))
            .sortBy(_.eventOccurredAt)
            .map(_.visited.id)


          val routeFirstVisits: List[MapRoute] = allVisitedRoutes
            .groupBy(_.visited)
            .iterator
            .map(v => v._1 -> v._2.minBy(_.eventOccurredAt))
            .map(_._2)
            .toList
            .filter(v => (v.eventOccurredAt.isEqual(event.startedAt) || v.eventOccurredAt.isAfter(event.startedAt) && (v.eventOccurredAt.isBefore(event.endedAt) || v.eventOccurredAt.isEqual(event.endedAt))))
            .sortBy(_.eventOccurredAt)
            .map(r => MapRoute(r.visited))


          Ok(
            views.html.visits.event.detail.index(
              request.user,
              visitedMapRoutes,
              visitedMapLocations,
              locationIdToVisitCount,
              routeToVisitCount,
              locationFirstVisits,
              routeFirstVisits,
              event,
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
              List.empty,
              List.empty,
              Event(name=s"Event not found for $id"),
              0
            )
          )
        )

  }

}
