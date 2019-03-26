package models.route

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.io.Source

class RoutesService {

  private val routes = RoutesService.makeRoutes(RoutesService.readRoutesFromFile)

  def getRoute(fromId: String, toId: String): Option[Route] =
    routes.find(r => r.from.id.equals(fromId) &&  r.to.id.equals(toId))


  def mapRoutes: Set[MapRoute] = {
    routes map {l => MapRoute(l)}
  }

  def defaultListRoutes: Set[ListRoute] = {
    routes map {l => ListRoute(l)}
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