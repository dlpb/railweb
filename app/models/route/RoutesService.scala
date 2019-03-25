package models.route

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

class RoutesService(routes: Set[Route]) {
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
  def makeRoutesService(routes: String): RoutesService = {
    implicit val formats = DefaultFormats
    new RoutesService(parse(routes).extract[Set[Route]]
    )
  }
}