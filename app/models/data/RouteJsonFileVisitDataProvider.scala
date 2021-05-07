package models.data

import com.typesafe.config.Config
import javax.inject.Inject
import models.data.file.JsonFileBasedVisitDataProvider
import models.route.Route
import services.route.RouteService

class RouteJsonFileVisitDataProvider @Inject()(config: Config, rs: RouteService
                                              )
  extends JsonFileBasedVisitDataProvider[Route, RouteVisit](config)
  with RouteDataProvider {
  override def dataPath: String = "route"

  override def routeService: RouteService = rs
}
