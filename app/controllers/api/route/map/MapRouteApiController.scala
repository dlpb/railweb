package controllers.api.route.map

import auth.api.{AuthorizedAction, UserRequest}
import javax.inject.{Inject, Singleton}
import models.auth.roles.{PlanUser, VisitUser}
import models.location.{LocationsService, MapLocation}
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.route.{MapRoute, RoutesService}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

@Singleton
class MapRouteApiController @Inject()(
                                       env: Environment,
                                       cc: ControllerComponents,
                                       locationService: LocationsService,
                                       routeService: RoutesService,
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
