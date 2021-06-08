package services.route

import com.typesafe.config.Config
import models.location.{Location, SpatialLocation}
import models.route.display.list.{ListRoute, ListRoutePoint}
import models.route.display.map.{MapRoute, MapRoutePoint}
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RouteServiceTest
  extends AnyFlatSpec with Matchers with MockitoSugar {

  val singleRoute = "/singleRoute.json"

  "Route Service" should "return a list of all routes in the json file" in {

    val service = getRouteServiceWith(singleRoute)

    val routes = service.routes

    routes.size should be(1)
  }

  it should "find a route by from and to location ids" in {
    val service = getRouteServiceWith(singleRoute)

    val from = "DRN"
    val to = "KYL"

    val foundRoute = service.findRoute(from, to)

    foundRoute.isDefined should be(true)
    foundRoute.get.from.id should be("DRN")
    foundRoute.get.to.id should be("KYL")
  }

  it should "not find a route by from and to location ids when they are provided back to front" in {
    val service = getRouteServiceWith(singleRoute)

    val to = "DRN"
    val from = "KYL"

    val foundRoute = service.findRoute(from, to)

    foundRoute.isDefined should be(false)
  }

  it should "not find a route by from and to location ids when they dont exist" in {
    val service = getRouteServiceWith(singleRoute)

    val to = "INV"
    val from = "WCK"

    val foundRoute = service.findRoute(from, to)

    foundRoute.isDefined should be(false)
  }

  it should "map routes to MapRoutes" in {
    val service = getRouteServiceWith(singleRoute)

    val routes = service.mapRoutes

    routes.size should be(1)
    routes.head should be(MapRoute(MapRoutePoint(57.31997511,-5.691316014, "Duirinish", "DRN"), MapRoutePoint(57.2797712,-5.713812582, "Kyle of Lochalsh", "KYL"), "SR", "P.12", "NR"))
  }

  it should "map routes to ListRoutes" in {
    val service = getRouteServiceWith(singleRoute)

    val routes = service.listRoutes

    routes.size should be(1)
    routes.head should be(ListRoute(ListRoutePoint("DRN", "Duirinish"), ListRoutePoint("KYL","Kyle of Lochalsh"), "P.12"))
  }

  it should "find routes for location" in {
    val service = getRouteServiceWith(singleRoute)

    val location = Location("KYL", "Kyle of Localsh", "SR", "NR", SpatialLocation(57.2797712,-5.713812582, None, None, None), None, true, Set("KYL"), Set("KYL"), None)
    val routesForLocation = service.findRoutesForLocation(location)

    routesForLocation.size should be(1)
  }

  it should "not find routes for location if there are no routes for that location" in {
    val service = getRouteServiceWith(singleRoute)

    val location = Location("RMF", "Romford", "LE", "NR", SpatialLocation(57.2797712,-5.713812582, None, None, None), None, true, Set("RMF"), Set("RMF"), None)
    val routesForLocation = service.findRoutesForLocation(location)

    routesForLocation.size should be(0)
  }

  private def getRouteServiceWith(routePath: String) = {
    val mockConfig = mock[Config]
    when(mockConfig.getString("data.routes.path")).thenReturn(routePath)
    val service = new RouteService(mockConfig)
    service
  }
}
