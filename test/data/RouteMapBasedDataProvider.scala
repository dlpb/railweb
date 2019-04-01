package data

import models.data.RouteDataProvider
import models.route.Route

class RouteMapBasedDataProvider extends MapBasedStorageProvider[Route] with RouteDataProvider {
  override def idToString(id: Route): String = s"""from:${id.from.id}-to:${id.to.id}"""
}
