package data

import models.route.Route

class RouteMapBasedDataProvider extends MapBasedStorageProvider[Route] {
  override def idToString(id: Route): String = s"""from:${id.from.id}-to:${id.to.id}"""
}
