package controllers.profile.visit.event

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.postgres.RouteDataIdConverter
import models.location.{LocationsService, MapLocation}
import models.route.{Route, RoutesService}
import models.visits.Event
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

@Singleton
class EventVisitsController @Inject()(
                                       userDao: UserDao,
                                       jwtService: JWTService,
                                       cc: ControllerComponents,
                                       locationsService: LocationsService,
                                       routesService: RoutesService,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       authorizedAction: AuthorizedAction
                                     ) extends AbstractController(cc) {


  def index = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val visitedRoutes = routesService.getVisitsForUser(request.user).getOrElse(Map.empty)
    val routeEvents: Set[String] = visitedRoutes.flatMap {
      _._2
    }.toSet
    val visitedLocations = locationsService.getVisitsForUser(request.user).getOrElse(Map.empty)
    val locationEvents: Set[String] = visitedLocations.flatMap {
      _._2
    }.toSet

    val events: List[String] = {
      routeEvents ++ locationEvents
    }.toList.sorted

    val eventsAndVisits: List[Event] = events map {
      event =>
        var visitedLocationsForEvent: Int = 0
        visitedLocations foreach {
          loc =>
            if (loc._2.contains(event)) visitedLocationsForEvent = visitedLocationsForEvent + 1
        }

        var visitedRoutesForEvent: Int = 0
        visitedRoutes foreach {
          route =>
            if (route._2.contains(event)) visitedRoutesForEvent = visitedRoutesForEvent + 1
        }

        Event(event, visitedRoutesForEvent, visitedLocationsForEvent)
    }

    Ok(views.html.visits.event.index(request.user, eventsAndVisits))

  }
}

