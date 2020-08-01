package models.plan.timetable.location

import java.io.FileNotFoundException
import java.time.ZonedDateTime

import javax.inject.Inject
import models.list.PathService
import models.location.LocationsService
import models.plan.timetable.reader.{Reader, WebZipInputStream}
import models.timetable.model.JsonFormats
import models.timetable.model.location.TimetableForLocation
import models.timetable.model.train._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class LocationTimetableService @Inject()(locationsService: LocationsService, pathService: PathService, ws: WSClient, reader: Reader = new WebZipInputStream) {

  private def getTrainsForLocationAroundNow(loc: String):LocationTimetableResult = {
    val from = LocationTimetableService.from
    val to = LocationTimetableService.to

    getTrainsForLocation(loc,
      from.getYear,
      from.getMonthValue,
      from.getDayOfMonth,
      from.getHour * 100 + from.getMinute,
      to.getHour * 100 + to.getMinute)
  }

  private def getDetailedTrainsForLocationAroundNow(loc: String): LocationTimetableResult = {
    val from = LocationTimetableService.from
    val to = LocationTimetableService.to

    getDetailedTrainsForLocation(loc,
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

  def getDetailedTrainsForLocation(loc: String,
                                   year: Int,
                                   month: Int,
                                   day: Int,
                                   from: Int,
                                   to: Int
                                  ): LocationTimetableResult = {
    (year,month,day,from,to) match {
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

case class LocationTimetableResult(timetables: Future[Seq[TimetableForLocation]], year: Int, month: Int, day: Int, from: Int, to: Int)

object LocationTimetableService {

  def from: ZonedDateTime = ZonedDateTime.now().minusMinutes(15)

  def to: ZonedDateTime = ZonedDateTime.now().plusMinutes(45)

  def hourMinute(time: Int) = {
    val hour = time / 100
    val minute = time % 100
    (hour, minute)
  }


  def isPublicCategory(category: TrainCategory) = {
    category.equals(OrdinaryLondonUndergroundMetroService) ||
      category.equals(UnadvertisedOrdinaryPassenger) ||
      category.equals(OrdinaryPassenger) ||
      category.equals(OrdinaryStaffTrain) ||
      category.equals(ExpressChannelTunnel) ||
      category.equals(ExpressSleeperEuropeNightServices) ||
      category.equals(ExpressInternational) ||
      category.equals(ExpressMotorail) ||
      category.equals(UnadvertisedExpress) ||
      category.equals(ExpressPassenger) ||
      category.equals(ExpressSleeperDomestic) ||
      category.equals(BusWtt) ||
      category.equals(BusReplacement) ||
      category.equals(Ship)
  }
}
