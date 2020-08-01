package models.plan.timetable.location

import java.io.FileNotFoundException

import javax.inject.Inject
import models.list.PathService
import models.location.{Location, LocationsService}
import models.plan.timetable.TimetableDateTimeHelper
import models.plan.timetable.reader.{Reader, WebZipInputStream}
import models.timetable.dto.TimetableHelper
import models.timetable.dto.location.detailed.DisplayDetailedLocationTimetable
import models.timetable.dto.location.simple.DisplaySimpleLocationTimetable
import models.timetable.model.JsonFormats
import models.timetable.model.location.TimetableForLocation
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.Result

import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.duration.Duration

class LocationTimetableService @Inject()(
                                          locationsService: LocationsService,
                                          pathService: PathService,
                                          ws: WSClient,
                                          reader: Reader = new WebZipInputStream) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def getDetailedTimetablesForLocation(loc: String,
                                       year: Int,
                                       month: Int,
                                       day: Int,
                                       from: Int,
                                       to: Int,
                                       date: String): LocationDetailedTimetableResult = {
    mapTimetablesToDisplayTimetables[DisplayDetailedLocationTimetable, LocationDetailedTimetableResult](
      loc,
      year,
      month,
      day,
      from,
      to,
      date,
      DisplayDetailedLocationTimetable.apply,
      LocationDetailedTimetableResult.apply)
  }

  def getSimpleTimetablesForLocation(loc: String,
                                     year: Int,
                                     month: Int,
                                     day: Int,
                                     from: Int,
                                     to: Int,
                                     date: String): LocationSimpleTimetableResult = {

    mapTimetablesToDisplayTimetables[DisplaySimpleLocationTimetable, LocationSimpleTimetableResult](
      loc,
      year,
      month,
      day,
      from,
      to,
      date,
      DisplaySimpleLocationTimetable.apply,
      LocationSimpleTimetableResult.apply)
  }


  private def mapTimetablesToDisplayTimetables[M,R](
                                                loc: String,
                                                year: Int,
                                                month: Int,
                                                day: Int,
                                                from: Int,
                                                to: Int,
                                                date: String,
                                                mappingFn: (LocationsService, TimetableForLocation, Int, Int, Int) => M,
                                                resultFn: (Future[Seq[M]], Int, Int, Int, Int, Int, Set[Location]) => R) = {
    val (y, m, d): (Int, Int, Int) = if (date.contains("-")) {
      val dateParts = date.split("-").map(_.toInt)
      (dateParts(0), dateParts(1), dateParts(2))
    } else (year, month, day)

    val locations = locationsService.findAllLocationsMatchingCrs(loc)

    println(s"Showing results for ${locations.map(_.tiploc)}")
    if (locations.nonEmpty) {
      val allTiplocResults = locations.map(location => getTrainsForLocation(location.id, y, m, d, from, to))

      val allTimetables: Seq[Future[Seq[M]]] = allTiplocResults.map(result => result.timetables map {
        f =>
          f.filter(tt => tt.publicStop && tt.publicTrain) map {
            t =>
              mappingFn(locationsService, t, result.year, result.month, result.day)
          }
      }).toSeq

      val timetables = Future.sequence(allTimetables).map(_.flatten)

      resultFn(timetables, allTiplocResults.headOption.map(_.year).getOrElse(y), allTiplocResults.headOption.map(_.month).getOrElse(m), allTiplocResults.headOption.map(_.day).getOrElse(d), allTiplocResults.headOption.map(_.from).getOrElse(from), allTiplocResults.headOption.map(_.to).getOrElse(to), locations)
    }
    else {
      resultFn(Future.successful(Seq.empty), y, m, d, from, to, locations)
    }
  }

  private def getTrainsForLocationAroundNow(loc: String): LocationTimetableResult = {
    val from = TimetableDateTimeHelper.from
    val to = TimetableDateTimeHelper.to

    getTrainsForLocation(loc,
      from.getYear,
      from.getMonthValue,
      from.getDayOfMonth,
      from.getHour * 100 + from.getMinute,
      to.getHour * 100 + to.getMinute)
  }

  def getTrainsForLocation(loc: String,
                           year: Int,
                           month: Int,
                           day: Int,
                           from: Int,
                           to: Int
                          ): LocationTimetableResult = {
    (year, month, day, from, to) match {
      case (0, 0, 0, -1, -1) => getTrainsForLocationAroundNow(loc)
      case _ => LocationTimetableResult(readTimetable(loc, year, month, day, from, to), year, month, day, from, to)
    }
  }

  private def readTimetable(loc: String,
                            year: Int,
                            month: Int,
                            day: Int,
                            from: Int,
                            to: Int
                           ): Future[Seq[TimetableForLocation]] = {
    implicit val formats = DefaultFormats ++ JsonFormats.formats

    try {
      val url = LocationTimetableServiceUrlHelper.createUrlForReadingLocationTimetables(loc, year, month, day, from, to)
      val request: WSRequest = ws.url(url)
      request
        .withRequestTimeout(Duration(30, "second"))
        .get()
        .map {
          response =>
            parse(response.body).extract[Seq[TimetableForLocation]]

        }(scala.concurrent.ExecutionContext.Implicits.global).recoverWith {
        case e =>
          println(s"Getting Simple Timetable error = ${e.getMessage}")
          Future.successful(Seq.empty)
      }(scala.concurrent.ExecutionContext.Implicits.global)
    }
    catch {
      case f: FileNotFoundException => println(s"No timetable for location $loc")
        Future.successful(Seq.empty)
      case e: Exception => println(s"Something went wrong: ${e.getMessage}")
        Future.successful(Seq.empty)
    }
  }
}

case class LocationSimpleTimetableResult(timetables: Future[Seq[DisplaySimpleLocationTimetable]], year: Int, month: Int, day: Int, from: Int, to: Int, locations: Set[Location])

case class LocationDetailedTimetableResult(timetables: Future[Seq[DisplayDetailedLocationTimetable]], year: Int, month: Int, day: Int, from: Int, to: Int, locations: Set[Location])

case class LocationTimetableResult(timetables: Future[Seq[TimetableForLocation]], year: Int, month: Int, day: Int, from: Int, to: Int)
