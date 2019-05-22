package controllers

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.postgres.RouteDataIdConverter
import models.location.{LocationsService, MapLocation}
import models.route.{MapRoute, Route, RoutesService}
import models.visits.Event
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.collection.immutable

@Singleton
class VisitsController @Inject()(
                                  userDao: UserDao,
                                  jwtService: JWTService,
                                  cc: ControllerComponents,
                                  locationsService: LocationsService,
                                  routesService: RoutesService,
                                  authenticatedUserAction: AuthorizedWebAction,
                                  authorizedAction: AuthorizedAction
                                ) extends AbstractController(cc) {

  def visitsPage = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    Ok(views.html.visits.index(request.user))

  }

  def visitsByLocationPage = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val locations = locationsService.getVisitsForUser(request.user).getOrElse(Map.empty[String, List[String]])
    Ok(views.html.visits.byLocation(request.user, locations))

  }

  def visitsByRoutePage = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val routes = routesService.getVisitsForUser(request.user).getOrElse(Map.empty[String, List[String]]) map {
      r =>
        RouteDataIdConverter.stringToRouteIds(r._1) -> r._2
    }
    val invalidRoutes: Set[(String, String)] = routes.keySet.filter(r => routesService.getRoute(r._1, r._2).isEmpty)
    Ok(views.html.visits.byRoute(request.user, routes, invalidRoutes))

  }

  def visitsByEventPage = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val visitedRoutes = routesService.getVisitsForUser(request.user).getOrElse(Map.empty)
    val routeEvents: Set[String] = visitedRoutes.flatMap {_._2}.toSet
    val visitedLocations = locationsService.getVisitsForUser(request.user).getOrElse(Map.empty)
    val locationEvents: Set[String] = visitedLocations.flatMap {_._2}.toSet

    val events: List[String] = {routeEvents ++ locationEvents}.toList.sorted

    val eventsAndVisits: List[Event] = events map {
      event =>
        var visitedLocationsForEvent: Int = 0
        visitedLocations foreach {
          loc =>
            if(loc._2.contains(event)) visitedLocationsForEvent = visitedLocationsForEvent + 1
        }

        var visitedRoutesForEvent: Int = 0
        visitedRoutes foreach {
          route =>
            if(route._2.contains(event)) visitedRoutesForEvent = visitedRoutesForEvent + 1
        }
        Event(event, visitedRoutesForEvent, visitedLocationsForEvent)
    }

    Ok(views.html.visits.byEvent(request.user, eventsAndVisits))

  }

  def visitsByEventDetailPage(event: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val visitedRoutes = routesService.getVisitsForUser(request.user)
      .getOrElse(Map.empty)
      .filter(_._2.contains(event))
      .map { route => RouteDataIdConverter.stringToRouteIds(route._1)}
      .flatMap { route => routesService.getRoute(route._1, route._2) map {MapRoute(_)}}
      .toList

    val visitedLocations = locationsService.getVisitsForUser(request.user)
      .getOrElse(Map.empty)
      .filter(_._2.contains(event))
      .flatMap { location => locationsService.getLocation(location._1) map { MapLocation(_) }}
      .toList

    Ok(views.html.visits.byEventDetail(request.user, visitedRoutes, visitedLocations, event))

  }

}

