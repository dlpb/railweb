package controllers.api.route

import auth.api.AuthorizedAction
import javax.inject.{Inject, Singleton}
import models.location.LocationsService
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.mvc.{AbstractController, ControllerComponents}
import services.route.RouteService

@Singleton
class RouteApiController @Inject()(
                                    env: Environment,
                                    cc: ControllerComponents,
                                    locationService: LocationsService,
                                    routeService: RouteService,
                                    trainService: LocationTimetableService,
                                    timetableService: TrainTimetableService,
                                    authAction: AuthorizedAction // NEW - add the action as a constructor argument
                                          )
  extends AbstractController(cc) {


    implicit val formats = DefaultFormats

    def getRoutesForList() = {
      authAction { implicit request =>
        Ok(write(routeService.listRoutes)).as(JSON)
      }
    }

    def getRoute(from: String, to: String) = {
      authAction { implicit request =>
        val route = routeService.findRoute(from, to)
        route match {
          case Some(routing) => Ok(write(routing)).as(JSON)
          case None => NotFound
        }
      }
    }
  }
