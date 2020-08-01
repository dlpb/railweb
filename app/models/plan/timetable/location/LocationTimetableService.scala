package models.plan.timetable.location

import java.io.FileNotFoundException

import javax.inject.Inject
import models.list.PathService
import models.location.{Location, LocationsService}
import models.plan.timetable.TimetableDateTimeHelper
import models.plan.timetable.reader.{Reader, WebZipInputStream}
import models.timetable.dto.location.detailed.DisplayDetailedLocationTimetable
import models.timetable.dto.location.simple.DisplaySimpleLocationTimetable
import models.timetable.model.JsonFormats
import models.timetable.model.location.TimetableForLocation
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class LocationTimetableService @Inject()(locationsService: LocationsService, pathService: PathService, ws: WSClient, reader: Reader = new WebZipInputStream) {




  private def getTrainsForLocationAroundNow(loc: String):LocationTimetableResult = {
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
    (year,month,day,from,to) match {
      case (0,0,0,-1,-1) => getTrainsForLocationAroundNow(loc)
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
