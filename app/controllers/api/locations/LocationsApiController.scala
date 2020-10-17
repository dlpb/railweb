package controllers.api.locations

import auth.api.{AuthorizedAction, UserRequest}
import javax.inject.{Inject, Singleton}
import models.auth.roles.{PlanUser, VisitUser}
import models.location.{LocationsService, MapLocation}
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.route.RoutesService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

@Singleton
class LocationsApiController @Inject()(
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

    def getLocationsForList() = {
      authAction { implicit request =>
        Ok(write(locationService.defaultListLocations)).as(JSON)
      }
    }

    def getLocation(id: String) = {
      authAction { implicit request =>
        val loc = locationService.getLocation(id)
        loc match {
          case Some(location) => Ok(write(location)).as(JSON)
          case None => NotFound
        }
      }
    }



  }
