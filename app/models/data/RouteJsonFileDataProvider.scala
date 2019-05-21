package models.data

import com.typesafe.config.Config
import javax.inject.Inject
import models.data.file.JsonFileBasedDataProvider
import models.route.Route

class RouteJsonFileDataProvider @Inject()(config: Config)
  extends JsonFileBasedDataProvider[Route](config)
  with RouteDataProvider {
  override def dataPath: String = "route"
  override def idToString(id: Route): String = s"""from:${id.from.id}-to:${id.to.id}"""
}
