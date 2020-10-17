package models.route

import java.io.InputStream

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.RouteDataProvider
import models.data.postgres.RouteDataIdConverter
import models.location.{Location, LocationsService}
import models.route.display.list.ListRoute
import models.route.display.map.MapRoute
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.io.Source

@Singleton
class RoutesService @Inject() ( config: Config,
                                dataProvider: RouteDataProvider) {
  def findRoutesForLocation(id: String): Set[Route] = {
    routes.filter { r =>
      r.from.id.equalsIgnoreCase(id) ||
      r.to.id.equalsIgnoreCase(id)
    }
  }

  private def dataRoot = config.getString("data.static.root")
  private val routes = makeRoutes(readRoutesFromFile)

  def getVisitsForUser(user: User): Option[Map[String, List[String]]] = {
    dataProvider.getVisits(user)
  }

  def saveVisits(visits: Option[Map[String, List[String]]], user: User) = {
    val nonEmptyVisits = visits.map(data => data.filter(_._2.nonEmpty))
    dataProvider.saveVisits(nonEmptyVisits, user)
  }

  def getRoute(fromId: String, toId: String): Option[Route] =
    routes.find(r => r.from.id.equalsIgnoreCase(fromId) && r.to.id.equalsIgnoreCase(toId))


  def mapRoutes: Set[MapRoute] = {
    routes map { l => MapRoute(l) }
  }

  def defaultListRoutes: Set[ListRoute] = {
    routes map { l => ListRoute(l) }
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
      .flatMap { route => getRoute(route._1, route._2) }
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

  def readRoutesFromFile: String = {
    val path = "/data/static/routes.json"
    val data: InputStream = getClass().getResourceAsStream(path)
    Source.fromInputStream(data).mkString
  }

  def migrate(oldRoute: Route, routes: List[Route], user: User): Unit = dataProvider.migrate(user, oldRoute, routes)

  def getRoutes = routes

  def makeRoutes(routes: String): Set[Route] = {
    implicit val formats = DefaultFormats
    parse(routes).extract[Set[Route]]
  }
}
