package services.visit.route

import java.time.LocalDateTime

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.{Event, RouteDataProvider, RouteVisit, Visit}
import models.route.Route
import services.route.RouteService
import services.visit.event.EventService

@Singleton
class RouteVisitService @Inject()(config: Config,
                                  routeService: RouteService,
                                  eventService: EventService,
                                  dataProvider: RouteDataProvider) {


  def getVisitsForUser(user: User): List[RouteVisit] = {
    dataProvider.getVisits(user).map(dataProvider.mapDataModelToMemoryModel)
  }

  def saveVisits(visits: List[RouteVisit], user: User) = {
    dataProvider.saveVisits(visits.map(dataProvider.mapMemoryModelToDataModel), user)
  }

  def saveVisitsAsJson(json: String, user: User) = dataProvider.saveVisits(json, user)

  def getVisitsAsJson(user: User) = dataProvider.getVisitsAsJson(user)

  def getVisitedRoutes(user: User): List[Route] = {
    getVisitsForUser(user)
      .map {
        data =>
          data.visited
    }
  }

  def getVisitsForRoute(route: Route, user: User): List[Visit[Route]] = {
    getVisitsForUser(user).filter(_.visited == route)
  }

  def visitRoute(route: Route, user: User): Unit = {
    eventService.ensureActiveEvent(user)
    dataProvider.saveVisit(dataProvider.mapMemoryModelToDataModel(RouteVisit(route, LocalDateTime.now(), LocalDateTime.now(), "MANUAL_VISIT")), user)
  }

  def deleteLastVisit(route: Route, user: User): Unit = {
    dataProvider.removeLastVisit(dataProvider.mapMemoryModelToDataModel(RouteVisit(route, LocalDateTime.MIN, LocalDateTime.MIN, "DELETE_LAST_VISIT")), user)
  }

  def deleteAllVisits(route: Route, user: User): Unit = {
    dataProvider.removeAllVisits(dataProvider.mapMemoryModelToDataModel(RouteVisit(route, LocalDateTime.MIN, LocalDateTime.MIN, "DELETE_ALL_VISITS")), user)
  }


  def getRoutesVisitedForEvent(event: Event, user: User): List[Route] = {

    val visits = getVisitsForUser(user)

    eventService.ensureAllVisitsHaveAnEvent(visits, user)

    visits
        .filter(v => v.eventOccurredAt.isBefore(event.endedAt) || v.eventOccurredAt.isEqual(event.endedAt))
        .filter(v => v.eventOccurredAt.isAfter(event.startedAt) || v.eventOccurredAt.isEqual(event.startedAt))
        .sortBy(_.eventOccurredAt)
        .map(_.visited)
    }

  def getEventsRouteWasVisited(route: Route, user: User): List[Event] = {
      val events = eventService.getEventsForUser(user)
      val routeVisits = getVisitsForRoute(route, user)

    eventService.ensureAllVisitsHaveAnEvent(routeVisits, user)


    val eventsForRoute: List[Event] = routeVisits
        .filter(_.visited.equals(route))
        .flatMap(v => {
        events
          .filter(e => v.eventOccurredAt.isBefore(e.endedAt) || v.eventOccurredAt.isEqual(e.endedAt))
          .filter(e => v.eventOccurredAt.isAfter(e.startedAt) || v.eventOccurredAt.isEqual(e.startedAt))
      })
      eventsForRoute
    }

}
