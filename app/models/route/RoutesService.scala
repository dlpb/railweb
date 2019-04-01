package models.route

import models.auth.User
import models.data.DataProvider
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.io.Source

class RoutesService(dataProvider: DataProvider[Route]) {

  private val routes = RoutesService.makeRoutes(RoutesService.readRoutesFromFile)

  def getRoute(fromId: String, toId: String): Option[Route] =
    routes.find(r => r.from.id.equals(fromId) && r.to.id.equals(toId))


  def mapRoutes: Set[MapRoute] = {
    routes map { l => MapRoute(l) }
  }

  def defaultListRoutes: Set[ListRoute] = {
    routes map { l => ListRoute(l) }
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

object RoutesService {
  def readRoutesFromFile: String = {
    Source.fromFile(System.getProperty("user.dir") + "/resources/data/static/routes.json").mkString
  }

  def makeRoutes(routes: String): Set[Route] = {
    implicit val formats = DefaultFormats
    parse(routes).extract[Set[Route]]
  }
}