package models.route.display.map

import models.route.Route

case class MapRoute(
                   from: MapRoutePoint,
                   to: MapRoutePoint,
                   toc: String,
                   srsCode: String,
                   `type`: String
                   )

object MapRoute {
  def apply(route: Route): MapRoute = {
    new MapRoute(
      MapRoutePoint(route.from.lat, route.from.lon, route.from.name, route.from.id),
      MapRoutePoint(route.to.lat, route.to.lon, route.to.name, route.to.id),
      route.toc,
      route.srsCode,
      route.`type`
     )
  }
}