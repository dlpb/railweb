package data

import models.location.Location

class LocationMapBasedDataProvider extends MapBasedStorageProvider[Location] {
  override def idToString(id: Location): String = id.id
}
