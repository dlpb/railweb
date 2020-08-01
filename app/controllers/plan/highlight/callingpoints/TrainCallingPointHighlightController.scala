package controllers.plan.highlight.callingpoints

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.list.PathService
import models.location.{LocationsService, MapLocation}
import models.plan.timetable.location.LocationTrainService
import models.plan.timetable.trains.TimetableService
import models.timetable.model.train.IndividualTimetable
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class TrainCallingPointHighlightController @Inject()(
                                     cc: ControllerComponents,
                                     authenticatedUserAction: AuthorizedWebAction,
                                     locationsService: LocationsService,
                                     pathService: PathService,
                                     trainService: LocationTrainService,
                                     timetableService: TimetableService,
                                     jwtService: JWTService

                              ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(trainsAndStations: String, srsLocations: String, locations: String, year: Int, month: Int, day: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())
      val mapLocations: List[MapLocation] =
        if(locations.isEmpty) { List.empty[MapLocation] }
        else locations
          .replaceAll("\\s+", ",")
          .split(",")
          .flatMap {
            locationsService.findLocation
          }
          .map {
            MapLocation(_)
          }.toList

      val srs = srsLocations
        .replaceAll("\\s+", ",")
        .split(",")

      val srsMapLocations: List[MapLocation] = if(srsLocations.isEmpty) List.empty[MapLocation] else locationsService
        .getLocations
        .filter {
          loc =>
            srs.contains(loc.nrInfo.map {
              _.srs
            }.getOrElse(" ")) ||
              srs.contains(loc.nrInfo.map {
                _.srs + " "
              }.getOrElse(" ").substring(0, 1))
        }
        .map {
          MapLocation(_)
        }

      val allStations = (srsMapLocations.toSet ++ mapLocations.toSet).toList

      val trainIdStationList: List[(String, String, String)] = trainsAndStations
        .linesWithSeparators
        .map {
          line =>
            val parts = line.split(",")
            (parts(0), parts(1), parts(2))
        }.toList

      val trainsF: List[Future[List[MapLocation]]] = trainIdStationList map {
        tsl =>
          val (train, from, to) = tsl
          Future {
            val tt = timetableService.getTrain(train, year.toString, month.toString, day.toString)
            val result: Future[List[MapLocation]] = tt.map {
              _.map {
                t: IndividualTimetable =>
                  val locs = t.locations.map {
                    _.tiploc
                  }.flatMap {
                    locationsService.findLocation
                  }
                  val fromIndexMaybe = locs.map(_.id).indexOf(from.trim)
                  val toIndexMaybe = locs.map(_.id).indexOf(to.trim)

                  val fromIndex = if(fromIndexMaybe < 0) 0 else fromIndexMaybe
                  val toIndex = if(toIndexMaybe < 0) locs.size - 1
                  else if (toIndexMaybe < locs.size) toIndexMaybe + 1
                  else toIndexMaybe


                  val sliced = locs
                    .slice(fromIndex, toIndex)
                    .map {
                      l => MapLocation(l)
                    }
                  sliced


              }.getOrElse(List.empty)
            }
            result

          }.flatten
      }
      val calledAt: List[MapLocation] = Await.result(Future.sequence(trainsF), Duration(30, "second")).flatten.toSet.toList
      val notCalledAt = allStations.diff(calledAt)
      val percentage = if(allStations.nonEmpty) calledAt.size*1.0d / allStations.size*1.0d else 100.0d
      Ok(views.html.plan.location.highlight.trains.index(request.user, token, calledAt, notCalledAt, allStations, percentage, trainsF.size, trainsAndStations, srsLocations, locations, List("Work In Progress - Plan - Highlight Locations"), year, month, day)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

}

