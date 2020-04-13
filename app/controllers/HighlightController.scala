package controllers

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.list.PathService
import models.location.{LocationsService, MapLocation}
import models.plan.timetable.TimetableService
import models.plan.trains.LocationTrainService
import models.timetable.model.train.IndividualTimetable
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class HighlightController @Inject()(
                                     cc: ControllerComponents,
                                     authenticatedUserAction: AuthorizedWebAction,
                                     locationsService: LocationsService,
                                     pathService: PathService,
                                     trainService: LocationTrainService,
                                     timetableService: TimetableService,
                                     jwtService: JWTService

                              ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def showLocationHighlightsForTrains(trainsAndStations: String, srsLocations: String, locations: String, year: Int, month: Int, day: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
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
            println(s"creating future for $train, $from, $to")
            val tt = timetableService.getTrain(train, year.toString, month.toString, day.toString)
            val result: Future[List[MapLocation]] = tt.map {
              _.map {
                t: IndividualTimetable =>
                  println(s"got timetable for $train")
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

                  println(s"filtered locations = ${locs.map(_.id)}")
                  println(s"index for $from is $fromIndex ($fromIndexMaybe)")
                  println(s"index for $to  is $toIndex ($toIndexMaybe)")
                  val sliced = locs
                    .slice(fromIndex, toIndex)
                    .map {
                      l => MapLocation(l)
                    }
                  println(s"sliced locations = ${sliced.map(_.id)}")
                  sliced


              }.getOrElse(List.empty)
            }
            result

          }.flatten
      }
      val calledAt: List[MapLocation] = Await.result(Future.sequence(trainsF), Duration(30, "second")).flatten.toSet.toList
      val notCalledAt = allStations.diff(calledAt)
      println(s"Final list is ${calledAt.map(_.id)}")
      val percentage = if(allStations.nonEmpty) calledAt.size*1.0d / allStations.size*1.0d else 100.0d
      Ok(views.html.plan.location.highlight.trains.index(request.user, token, calledAt, notCalledAt, allStations, percentage, trainsF.size, trainsAndStations, srsLocations, locations, List("Work In Progress - Plan - Highlight Locations"))(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showLocationHighlights(locations: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(PlanUser)){
      val token = jwtService.createToken(request.user, new Date())
      val locIds = locations
        .replaceAll("\\s+", ",")
        .split(",")
        .flatMap {locationsService.getLocation}
        .map { _.id }
      Ok(views.html.plan.location.highlight.index(request.user, token, locIds.toList, List("Work In Progress - Plan - Highlight Locations"))(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}

