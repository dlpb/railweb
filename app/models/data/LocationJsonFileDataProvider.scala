package models.data

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.data.file.JsonFileBasedDataProvider
import models.location.Location

@Singleton
class LocationJsonFileDataProvider @Inject() (config: Config)
  extends JsonFileBasedDataProvider[Location](config)
    with LocationDataProvider {
  override def dataPath: String = "location"
  override def idToString(id: Location): String = id.id
}
