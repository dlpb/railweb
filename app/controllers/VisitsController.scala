package controllers

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.postgres.RouteDataIdConverter
import models.location.{Location, LocationsService, MapLocation}
import models.route.{MapRoute, Route, RoutesService}
import models.visits.Event
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.collection.immutable
import scala.collection.immutable.ListMap

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
    val locations: Map[String, List[String]] =
      locationsService
        .getVisitsForUser(request.user)
        .getOrElse(Map.empty[String, List[String]])
    val mapLocations: List[MapLocation] = locations
        .keySet
        .flatMap { locationsService.getLocation }
        .map { MapLocation(_) }
        .toList

    Ok(views.html.visits.byLocation(request.user, locations, mapLocations))

  }

  def visitsByRoutePage = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val routes: Map[(String, String), List[String]] = routesService.getVisitsForUser(request.user)
      .getOrElse(Map.empty[String, List[String]])
      .filter(_._2.nonEmpty)
      .map {
      r =>
        RouteDataIdConverter.stringToRouteIds(r._1) -> r._2
    }

    println(routes)
    val invalidRoutes: Set[(String, String)] = routes.keySet.filter(r => routesService.getRoute(r._1, r._2).isEmpty)

    val mapRoutes: List[MapRoute] = routes
        .filter(_._2.nonEmpty)
        .keySet
        .flatMap { r => routesService.getRoute(r._1, r._2) }
        .map { MapRoute(_) }
        .toList
        .sortBy(r => r.from.id + " - " + r.to.id)

    Ok(views.html.visits.byRoute(request.user, routes, invalidRoutes, mapRoutes))

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
    val visitedRoutes: List[Route] = routesService.getRoutesVisitedForEvent(event, request.user)
    val visitedLocations = locationsService
      .getLocationsVisitedForEvent(event, request.user)
      .map({ MapLocation(_) })
      .sortBy(_.name)

    val distance: Long = visitedRoutes
      .map({ _.distance })
      .sum

    val visitedMapRoutes = visitedRoutes
      .map { MapRoute(_) }
        .sortBy(r => r.from.id + " - " + r.to.id)

    Ok(views.html.visits.byEventDetail(request.user, visitedMapRoutes, visitedLocations, event, distance))

  }

}

