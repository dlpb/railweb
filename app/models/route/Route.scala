package models.route

case class Route(
                from: RoutePoint,
                to: RoutePoint,
                toc: String,
                singleTrack: String,
                electrification: String,
                speed: String,
                srsCode: String,
                `type`: String
                )

case class RoutePoint(
                     lat: Double,
                     lon: Double,
                     id: String,
                     name: String,
                     `type`: String
                     )

case class MapRoute(
                   from: MapRoutePoint,
                   to: MapRoutePoint,
                   toc: String,
                   srsCode: String,
                   `type`: String
                   )

case class MapRoutePoint(lat: Double, lon: Double, name: String, id: String)

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

case class ListRoute(from: ListRoutePoint, to: ListRoutePoint, srs: String)

case class ListRoutePoint(id: String, name: String)

object ListRoute {
  def apply(route: Route): ListRoute = {
    new ListRoute(
      ListRoutePoint(route.from.id, route.from.name),
      ListRoutePoint(route.to.id, route.to.name),
      route.srsCode
    )
  }
}