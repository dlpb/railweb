package models.route

import java.io.InputStream

import com.typesafe.config.Config
import javax.inject.Inject
import models.auth.User
import models.data.RouteDataProvider
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.io.Source

class RoutesService @Inject() ( config: Config,
                                dataProvider: RouteDataProvider) {


  private def dataRoot = config.getString("data.static.root")
  private val routes = makeRoutes(readRoutesFromFile)

  def getRoute(fromId: String, toId: String): Option[Route] =
    routes.find(r => r.from.id.equals(fromId) && r.to.id.equals(toId))


  def mapRoutes: Set[MapRoute] = {
    routes map { l => MapRoute(l) }
  }

  def defaultListRoutes: Set[ListRoute] = {
    routes map { l => ListRoute(l) }
  }
  def getVisitedRoutes(user: User): List[String] = {
    dataProvider.getVisits(user).map {
      data =>
        data.keySet.toList
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

  def visitRoute(route: Route, user: User): Unit = {
    println(s"Route tracing == route found, saving $route")
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

  def makeRoutes(routes: String): Set[Route] = {
    implicit val formats = DefaultFormats
    parse(routes).extract[Set[Route]]
  }
}