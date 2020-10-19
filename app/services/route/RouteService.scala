package services.route

import com.typesafe.config.Config
import javax.inject.Inject
import models.helpers.JsonFileReader
import models.location.Location
import models.route.Route
import models.route.display.list.ListRoute
import models.route.display.map.MapRoute

class RouteService @Inject() ( config: Config ) {


  val routeFileReader = new JsonFileReader

  val routes: Set[Route] = routeFileReader.readAndParse[Set[Route]](config.getString("data.routes.path"))

  val mapRoutes: Set[MapRoute] = routes.map(r => MapRoute(r))

  val listRoutes: Set[ListRoute] = routes.map(r => ListRoute(r))

  def findRoute(from: String, to: String): Option[Route] = routes.find(r => r.from.id.equals(from) && r.to.id.equals(to))

  def findRoutesForLocation(location: Location): Set[Route] = {
    routes.filter { r =>
      r.from.id.equalsIgnoreCase(location.id) ||
        r.to.id.equalsIgnoreCase(location.id)
    }
  }
}
