package services.route

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.helpers.JsonFileReader
import models.location.Location
import models.route.{Route, RouteDetail}

import scala.util.Try

@Singleton
class RouteService @Inject() ( config: Config ) {


  val routeFileReader = new JsonFileReader

  val routes: Set[Route] = {
    val routeListPath = config.getString("data.routes.path")
    val routeFileName = config.getString("data.routes.fileName")

    val routesPath = s"$routeListPath$routeFileName"
    val routesSet = routeFileReader.readAndParse[Set[Route]](routesPath)
    System.gc()
    routesSet
  }

  def getRouteExtraDetails(from: String, to: String): Set[RouteDetail] = {
    val routePath = config.getString("data.routes.path")
    val routeDetailPath = config.getString("data.routes.details")

    val routesPath = s"$routePath$routeDetailPath"
    val permutation1 = s"$from-$to"
    val permutation2 = s"$to-$from"

    val details: Set[RouteDetail] = try {
      routeFileReader.readAndParse[Set[RouteDetail]](routesPath + permutation1)
    }
    catch {
      case _: Exception =>
        try {
          routeFileReader.readAndParse[Set[RouteDetail]](routesPath + permutation2)
        }
      catch {
        case _: Exception => Set.empty
      }
    }
    System.gc()
    details
  }

  def findRoute(from: String, to: String): Option[Route] = routes.find(r => r.from.id.equals(from) && r.to.id.equals(to))

  def findRoutesForLocation(location: Location): Set[Route] = {
    routes.filter { r =>
      r.from.id.equalsIgnoreCase(location.id) ||
        r.to.id.equalsIgnoreCase(location.id)
    }
  }
}
