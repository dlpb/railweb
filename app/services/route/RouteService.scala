package services.route

import com.typesafe.config.Config
import javax.inject.Inject
import models.route.Route

class RouteService @Inject() ( config: Config ) {

  def getAllRoutes(): List[Route] = List.empty
}
