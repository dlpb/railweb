package controllers.api.plan.route

import auth.api.{AuthorizedAction, UserRequest}
import javax.inject.{Inject, Singleton}
import models.auth.roles.{PlanUser, VisitUser}
import models.location.{LocationsService, MapLocation}
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.route.RoutesService
import models.route.display.map.MapRoute
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

@Singleton
class PlanRoutesApiController @Inject()(
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

    def createMapRouteForTrain(train: String, year: Int, month: Int, day: Int) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(PlanUser)) Unauthorized("User does not have the right role")
        else {
          val eventualResult: Future[Option[List[MapRoute]]] = timetableService.getTrain(train,  year.toString, month.toString, day.toString).map {
            f =>
              f map {
                t =>
                  timetableService.createSimpleMapRoutes(t)
              }
          }(scala.concurrent.ExecutionContext.Implicits.global)
          try {
            val mapDetails = Await.result(eventualResult, Duration(30, "second")).getOrElse(List())

            Ok(write(mapDetails))
          }
          catch{
            case e: TimeoutException =>
              println("timed out")
              InternalServerError(s"Could not get Map Detailed Map Details for $train")
          }
        }
      }
    }


  }
