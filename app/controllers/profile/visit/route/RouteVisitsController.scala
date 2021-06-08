package controllers.profile.visit.route

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.{Event, RouteVisit, Visit}
import models.route.Route
import models.route.display.map.MapRoute
import play.api.mvc.{AbstractController, AnyContent, Call, ControllerComponents}
import services.route.RouteService
import services.visit.route.RouteVisitService

@Singleton
class RouteVisitsController @Inject()(
                                       userDao: UserDao,
                                       jwtService: JWTService,
                                       cc: ControllerComponents,
                                       routesService: RouteService,
                                       routeVisitService: RouteVisitService,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       authorizedAction: AuthorizedAction
                                     ) extends AbstractController(cc) {

  def index(sortField: String, sortOrder: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val routeVisits: List[RouteVisit] = routeVisitService.getVisitsForUser(request.user).distinctBy(_.visited)

    val routeVisitEvents: Map[(String, String), List[Event]] = routeVisits.map(visit => {
      val events = routeVisitService.getEventsRouteWasVisited(visit.visited, request.user).distinctBy(_.id)
      (visit.visited.from.id, visit.visited.to.id) -> events
    }).toMap

    val mapRoutes: List[MapRoute] = routeVisits
      .map { r => r.visited }
      .map {
        MapRoute(_)
      }
      .sortBy(r => r.from.id + " - " + r.to.id)

    val allVisitedRoutes: List[RouteVisit] = routeVisitService.getVisitsForUser(request.user)

    val routeVisitCount: Map[Route, Int] = allVisitedRoutes
      .groupBy(_.visited)
      .iterator
      .map(l => l._1 -> l._2.size)
      .toMap

    val routeVisitIndex: Map[Route, Int] = allVisitedRoutes        //get all visits for all routes
      .groupBy(_.visited)                                           //group by routes
      .iterator
      .map(l => {                                                   //map over the group of routes to visit
        val location = l._1
        val visits = l._2
        val sortedVisits = visits.sortBy(_.eventOccurredAt)         //sort visits by date
        val earliestVisit = sortedVisits.head                       //take the first
        location -> earliestVisit.eventOccurredAt                   //make a new map of routes to first visit
      })
      .toList
      .sortBy(_._2)                                                 //sort new map by first visits
      .map(_._1)                                                    //drop the visit date as it's now not needed
      .zipWithIndex                                                 //zip with index to get the order
      .map(l => l._1 -> (l._2 + 1))                                 //add one to make it human readable
      .toMap


    val sortedVisits = (sortField, sortOrder) match {
      case ("date", "asc") => routeVisits.sortBy(_.eventOccurredAt)
      case ("date", "desc") => routeVisits.sortBy(_.eventOccurredAt).reverse
      case ("name", "asc") => routeVisits.sortBy(r => r.visited.from.name + "-" + r.visited.to.name)
      case ("name", "desc") => routeVisits.sortBy(r => r.visited.from.name + "-" + r.visited.to.name).reverse
      case ("id", "asc") => routeVisits.sortBy(r => r.visited.from.id + "-" + r.visited.to.id)
      case ("id", "desc") => routeVisits.sortBy(r => r.visited.from.id + "-" + r.visited.to.id).reverse
      case ("count", "asc") => {
        routeVisits.map(v => {
          val count = routeVisitCount(v.visited)
          v -> count
        })
          .sortBy(_._2)
          .map(_._1)
      }

      case ("count", "desc") => {
        routeVisits.map(v => {
          val count = routeVisitCount(v.visited)
          v -> count
        })
          .sortBy(_._2)
          .reverse
          .map(_._1)
      }
      case _ => routeVisits.sortBy(r => r.visited.from.name + "-" + r.visited.to.name)
    }

    val call: Call =  routes.RouteVisitsController.index(sortField, sortOrder)

    Ok(views.html.visits.route.index(request.user, sortedVisits, routeVisitEvents, mapRoutes, routeVisitCount, routeVisitIndex, call, sortField, sortOrder))

  }
}

