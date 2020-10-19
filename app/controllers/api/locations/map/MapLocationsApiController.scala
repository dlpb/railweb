package controllers.api.locations.map

import auth.api.{AuthorizedAction, UserRequest}
import javax.inject.{Inject, Singleton}
import models.auth.roles.{PlanUser, VisitUser}
import models.location.{LocationsService, MapLocation}
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.visits.route.RouteVisitService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

@Singleton
class MapLocationsApiController @Inject()(
                                           env: Environment,
                                           cc: ControllerComponents,
                                           locationService: LocationsService,
                                           routeService: RouteVisitService,
                                           trainService: LocationTimetableService,
                                           timetableService: TrainTimetableService,
                                           authAction: AuthorizedAction // NEW - add the action as a constructor argument
                                          )
  extends AbstractController(cc) {

    implicit val formats = DefaultFormats

    def getLocationsForMap() = {
      authAction { implicit request: UserRequest[AnyContent] =>
        Ok(write(locationService.mapLocations)).as(JSON)
      }
    }
  }
