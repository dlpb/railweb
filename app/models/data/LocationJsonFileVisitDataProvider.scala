package models.data

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.data.file.JsonFileBasedVisitDataProvider
import models.location.Location
import services.location.LocationService

@Singleton
class LocationJsonFileVisitDataProvider @Inject()(config: Config, ls: LocationService)
  extends JsonFileBasedVisitDataProvider[Location, LocationVisit](config)
    with LocationDataProvider {
  override def dataPath: String = "location"

  override def locationService: LocationService = ls
}
