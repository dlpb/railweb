package controllers.api.route.map

import auth.api.AuthorizedAction
import javax.inject.{Inject, Singleton}
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.mvc.{AbstractController, ControllerComponents}
import services.route.RouteService

@Singleton
class MapRouteApiController @Inject()(
                                       env: Environment,
                                       cc: ControllerComponents,
                                       routeService: RouteService,
                                       trainService: LocationTimetableService,
                                       timetableService: TrainTimetableService,
                                       authAction: AuthorizedAction // NEW - add the action as a constructor argument
                                          )
  extends AbstractController(cc) {


    implicit val formats = DefaultFormats

    def getRoutesForMap() = {
      authAction { implicit request =>
        Ok(write(routeService.mapRoutes)).as(JSON)
      }
    }

  }
