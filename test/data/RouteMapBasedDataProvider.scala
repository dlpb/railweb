package data

import models.auth.User
import models.data.RouteDataProvider
import models.route.Route

class RouteMapBasedDataProvider extends MapBasedStorageProvider[Route] with RouteDataProvider {
  override def idToString(id: Route): String = s"""from:${id.from.id}-to:${id.to.id}"""

  override def saveVisits(visits: Option[Map[String, List[String]]], user: User): Unit = {}
}
