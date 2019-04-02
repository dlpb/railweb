package data

import models.data.LocationDataProvider
import models.location.Location

class LocationMapBasedDataProvider extends MapBasedStorageProvider[Location] with LocationDataProvider {
  override def idToString(id: Location): String = id.id
}
