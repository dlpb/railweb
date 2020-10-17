package models.route.display.map

import models.route.{Route, RoutePoint}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MapRouteTest extends AnyFlatSpec with Matchers {
  "Map Route" should "create a map route from a route" in {
    val route = Route(RoutePoint(0, 0, "from", "from", "from"), RoutePoint(1, 1, "to", "to", "to"), "toc", "single", "electrification", "speed", "srs", "type", 0)
    val mapRoute = MapRoute(route)

    mapRoute.from should be(MapRoutePoint(0,0,"from", "from"))
    mapRoute.to should be(MapRoutePoint(1,1,"to", "to"))
    mapRoute.`type` should be("type")
    mapRoute.srsCode should be("srs")
    mapRoute.toc should be("toc")
  }
}
