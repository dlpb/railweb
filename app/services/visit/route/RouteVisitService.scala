package services.visit.route

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.RouteDataProvider
import models.data.postgres.RouteDataIdConverter
import models.route.Route
import services.route.RouteService

@Singleton
class RouteVisitService @Inject()(config: Config,
                                  routeService: RouteService,
                                  dataProvider: RouteDataProvider) {


  def getVisitsForUser(user: User): Option[Map[String, List[String]]] = {
    dataProvider.getVisits(user)
  }

  def saveVisits(visits: Option[Map[String, List[String]]], user: User) = {
    val nonEmptyVisits = visits.map(data => data.filter(_._2.nonEmpty))
    dataProvider.saveVisits(nonEmptyVisits, user)
  }


  def getVisitedRoutes(user: User): List[String] = {
    dataProvider.getVisits(user)
      .map {
        data =>
          data.filter(d => d._2.nonEmpty).keySet.toList
    }.getOrElse(List())
  }

  def getVisitsForRoute(route: Route, user: User): List[String] = {
    dataProvider.getVisits(user) flatMap {
      _.get(dataProvider.idToString(route))
    } match {
      case Some(list) =>
        list
      case None =>
        List()
    }
  }

  def getRoutesVisitedForEvent(event: String, user: User): List[Route] = {
    getVisitsForUser(user)
      .getOrElse(Map.empty)
      .filter(_._2.contains(event))
      .keySet
      .map { route => RouteDataIdConverter.stringToRouteIds(route)}
      .flatMap { route => routeService.findRoute(route._1, route._2) }
      .toList
  }

  def visitRoute(route: Route, user: User): Unit = {
    dataProvider.saveVisit(route, user)
  }

  def deleteLastVisit(route: Route, user: User): Unit = {
    dataProvider.removeLastVisit(route, user)
  }

  def deleteAllVisits(route: Route, user: User): Unit = {
    dataProvider.removeAllVisits(route, user)
  }

}
