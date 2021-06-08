package controllers.api.plan.highlight.callingpoints

import java.time.LocalTime

import auth.api.AuthorizedAction
import javax.inject.{Inject, Singleton}
import models.auth.roles.PlanUser
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.mvc.{AbstractController, ControllerComponents}
import services.location.LocationService

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future, TimeoutException}

@Singleton
class PlanCallingPointHighlightApiController @Inject()(
                                                        env: Environment,
                                                        locationsService: LocationService,
                                                        cc: ControllerComponents,
                                                        trainTimetableService: TrainTimetableService,
                                                        locationTimetableService: LocationTimetableService,
                                                        authAction: AuthorizedAction // NEW - add the action as a constructor argument
                                          )
  extends AbstractController(cc) {

    implicit val formats = DefaultFormats

  private val timeout: FiniteDuration = Duration(30, "second")

  def getTrainsAtStationForToday(location: String, date: String, hasCalledAt: String, willCallAt: String) = {

      import scala.concurrent.ExecutionContext.Implicits.global

      authAction { implicit request =>
        if (!request.user.roles.contains(PlanUser)) Unauthorized("User does not have the right role")
        else if(location.length < 2) BadRequest("Train ID Must be 5 characters long, e.g. P12345")
        else if(!date.contains("-")) BadRequest("Date must be in the format yyyy-mm-dd")
        else {
          val (y, m, d): (Int, Int, Int) = if (date.contains("-")) {
            val dateParts = date.split("-").map(_.toInt)
            (dateParts(0), dateParts(1), dateParts(2))
          } else (0, 0, 0)

          val locs = locationsService.findAllLocationsByCrs(location).flatMap(l => locationsService.findAllLocationsByCrs(l.crs.mkString))
          if(locs.isEmpty) BadRequest(s"location $location not found")
          println(s"trying to get timetable for location $location (${locs.map(_.id)}) on date $y $m $d with hasCalledAt $hasCalledAt and willCallAt $willCallAt")


          val willCallAtLoc = if(!willCallAt.isBlank) locationsService.findFirstLocationByNameTiplocCrsOrId(willCallAt).map(_.id) else None
          val hasCalledAtLoc = if(!hasCalledAt.isBlank) locationsService.findFirstLocationByNameTiplocCrsOrId(hasCalledAt).map(_.id) else None

          val eventualResult: Future[Seq[HighlightLocationTimetableEntry]] = Future.sequence(locs.map(l => locationTimetableService.getTrainsForLocation(l.id, y, m, d, 0, 2400, hasCalledAtLoc, willCallAtLoc).timetables.map {
              timetables =>
                timetables
                  .filter(t => t.publicTrain && t.publicStop)
                  .map(t => HighlightLocationTimetableEntry(
                    t.uid,
                    t.origin.flatMap(locationsService.findFirstLocationByNameTiplocCrsOrId).map(_.name).getOrElse(t.origin.getOrElse("")),
                    t.destination.flatMap(locationsService.findFirstLocationByNameTiplocCrsOrId).map(_.name).getOrElse(t.destination.getOrElse("")),
                    t.pubArr.map(_.toString).getOrElse(t.arr.map(_.toString).getOrElse("")),
                    t.pubDep.map(_.toString).getOrElse(t.dep.map(_.toString).getOrElse("")),
                    t.platform.getOrElse(""),
                    t.pubDep.map(_.toString).getOrElse(t.dep.map(_.toString).getOrElse(t.pubArr.map(_.toString).getOrElse(t.arr.map(_.toString).getOrElse(""))))
                  ))
            })
              .toSeq)
            .map(_
              .flatten
              .sortBy(_.time))
          try {
            val locationTimetables: Seq[HighlightLocationTimetableEntry] = Await.result(eventualResult, timeout)

            Ok(write(locationTimetables))
          }
          catch{
            case e: TimeoutException =>
              InternalServerError(s"Could not timetables for location $location")
          }
        }
      }
    }

    def getTrainTimetableCallingPoints(train: String, date: String) = {


      authAction { implicit request =>
        if (!request.user.roles.contains(PlanUser)) Unauthorized("User does not have the right role")
        else if(train.length != 6) BadRequest("Train ID Must be 5 characters long, e.g. P12345")
        else if(!date.contains("-")) BadRequest("Date must be in the format yyyy-mm-dd")
        else {
          val (y, m, d): (Int, Int, Int) = if (date.contains("-")) {
            val dateParts = date.split("-").map(_.toInt)
            (dateParts(0), dateParts(1), dateParts(2))
          } else (0, 0, 0)
          println(s"trying to get timetable for train $train on date $y $m $d")

          val eventualResult: Future[Option[List[HighlightTrainTimetableEntry]]] = trainTimetableService.getTrain(train, y.toString, m.toString, d.toString).map {
            f =>
              f map {
                t =>
                  t.locations
                    .filter(l => l.publicArrival.isDefined || l.publicDeparture.isDefined)
                    .map {
                    loc =>
                      val dep = loc.publicDeparture.getOrElse(loc.departure.getOrElse(LocalTime.MIDNIGHT))
                      val arr = loc.publicArrival.getOrElse(loc.arrival.getOrElse(LocalTime.MIDNIGHT))
                      val timetableLocation = locationsService.findLocationByNameTiplocCrsIdPrioritiseOrrStations(loc.tiploc)

                      val optionString: String = timetableLocation.map(l => {
                        val crs = l.crs.headOption.getOrElse(loc.tiploc)
                        val line = "["+ crs + "] " + l.name
                        line
                      }).getOrElse(loc.tiploc.toString)

                      HighlightTrainTimetableEntry(
                        loc.tiploc,
                        optionString,
                        dep.toString,
                        arr.toString,
                        (loc.publicDeparture, loc.publicArrival) match {
                          case (Some(_), None) => "Terminus"
                          case (None, Some(_)) => "Origin"
                          case _ => "Intermediate"
                        }
                      )
                  }
              }
          }(scala.concurrent.ExecutionContext.Implicits.global)
          try {
            val timetableCallingPoints = Await.result(eventualResult, timeout).getOrElse(List())

            Ok(write(timetableCallingPoints))
          }
          catch{
            case e: TimeoutException =>
              InternalServerError(s"Could not get timetables for train $train")
          }
        }
      }
    }

  }

case class HighlightTrainTimetableEntry(tiploc: String, name: String, dep: String, arr: String, `type`: String)
case class HighlightLocationTimetableEntry(uid: String, org: String, dst: String, arr: String, dep: String, pfm: String, time: String)