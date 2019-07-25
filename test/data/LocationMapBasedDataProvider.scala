package data

import models.auth.User
import models.data.LocationDataProvider
import models.location.Location

class LocationMapBasedDataProvider extends MapBasedStorageProvider[Location] with LocationDataProvider {
  override def idToString(id: Location): String = id.id

  override def saveVisits(visits: Option[Map[String, List[String]]], user: User): Unit = ???
}
