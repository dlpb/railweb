package models.route.display.list

import models.route.Route

case class ListRoute(from: ListRoutePoint, to: ListRoutePoint, srs: String)

object ListRoute {
  def apply(route: Route): ListRoute = {
    new ListRoute(
      ListRoutePoint(route.from.id, route.from.name),
      ListRoutePoint(route.to.id, route.to.name),
      route.srsCode
    )
  }
}