package models.plan.trains

import java.io.FileNotFoundException
import java.time.ZonedDateTime

import javax.inject.Inject
import models.list.PathService
import models.location.LocationsService
import models.plan.reader.{Reader, WebZipInputStream}
import models.timetable.model.JsonFormats
import models.timetable.model.location.TimetableForLocation
import models.timetable.model.train._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class LocationTrainService @Inject()(locationsService: LocationsService, pathService: PathService, ws: WSClient, reader: Reader = new WebZipInputStream) {

  def getTrainsForLocationAroundNow(loc: String): (Future[Seq[TimetableForLocation]], (Int, Int, Int, Int, Int)) = {
    val from = LocationTrainService.from
    val to = LocationTrainService.to

    (getTrainsForLocation(loc,
      from.getYear,
      from.getMonthValue,
      from.getDayOfMonth,
      from.getHour * 100 + from.getMinute,
      to.getHour * 100 + to.getMinute
    ), (from.getYear, from.getMonthValue, from.getDayOfMonth, from.getHour * 100 + from.getMinute, to.getHour * 100 + to.getMinute))
  }

  def getDetailedTrainsForLocationAroundNow(loc: String): (Future[Seq[TimetableForLocation]], (Int, Int, Int, Int, Int)) = {
    val from = LocationTrainService.from
    val to = LocationTrainService.to

    (getDetailedTrainsForLocation(loc,
      from.getYear,
      from.getMonthValue,
      from.getDayOfMonth,
      from.getHour * 100 + from.getMinute,
      to.getHour * 100 + to.getMinute
    ), (from.getYear, from.getMonthValue, from.getDayOfMonth, from.getHour * 100 + from.getMinute, to.getHour * 100 + to.getMinute))
  }

  def getTrainsForLocation(loc: String,
                           year: Int,
                           month: Int,
                           day: Int,
                           from: Int,
                           to: Int
                          ): Future[Seq[TimetableForLocation]] = {
    readTimetable(loc, year, month, day, from, to)
  }

  def getDetailedTrainsForLocation(loc: String,
                                   year: Int,
                                   month: Int,
                                   day: Int,
                                   from: Int,
                                   to: Int
                                  ): Future[Seq[TimetableForLocation]] = {
    readTimetable(loc, year, month, day, from, to)
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
      val url = LocationTrainService.createUrlForReadingLocationTimetables(loc, year, month, day, from, to)
      println(s"trying to get something for URL $url")
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

object LocationTrainService {

  def from: ZonedDateTime = ZonedDateTime.now().minusMinutes(15)

  def to: ZonedDateTime = ZonedDateTime.now().plusMinutes(45)

  def hourMinute(time: Int) = {
    val hour = time / 100
    val minute = time % 100
    (hour, minute)
  }

  def createUrlForReadingLocationTimetables(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int) = {
    val m = if (month < 1) "01" else if (month < 10) s"0$month" else if (month > 12) "12" else s"$month"
    val d = if (day < 1) "01" else if (day < 10) s"0$day" else if (day > 31) "31" else s"$day"
    val f = if (from < 0) "0000" else if (from < 10) s"000$from" else if (from < 100) s"00$from" else if (from < 1000) s"0$from" else if (from > 2400) "2400" else s"$from"
    val t = if (to < 0) "0000" else if (to < 10) s"000$to" else if (to < 100) s"00$to" else if (to < 1000) s"0$to" else if (to > 2400) "2400" else s"$to"
    val url = s"http://railweb-timetables-java.herokuapp.com/timetables/location/$loc?year=$year&month=$m&day=$d&from=$f&to=$t"
    url
  }

  def createUrlForDisplayingLocationSimpleTimetables(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int) = {
    val m = if (month < 1) "01" else if (month < 10) s"0$month" else if (month > 12) "12" else s"$month"
    val d = if (day < 1) "01" else if (day < 10) s"0$day" else if (day > 31) "31" else s"$day"
    val f = if (from < 0) "0000" else if (from < 10) s"000$from" else if (from < 100) s"00$from" else if (from < 1000) s"0$from" else if (from > 2400) "2400" else s"$from"
    val t = if (to < 0) "0000" else if (to < 10) s"000$to" else if (to < 100) s"00$to" else if (to < 1000) s"0$to" else if (to > 2400) "2400" else s"$to"
    val url = s"/plan/location/trains/simple/$loc?year=$year&month=$m&day=$d&from=$f&to=$t"
    url
  }

  def createUrlForDisplayingLocationDetailedTimetables(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int) = {
    val m = if (month < 1) "01" else if (month < 10) s"0$month" else if (month > 12) "12" else s"$month"
    val d = if (day < 1) "01" else if (day < 10) s"0$day" else if (day > 31) "31" else s"$day"
    val f = if (from < 0) "0000" else if (from < 10) s"000$from" else if (from < 100) s"00$from" else if (from < 1000) s"0$from" else if (from > 2400) "2400" else s"$from"
    val t = if (to < 0) "0000" else if (to < 10) s"000$to" else if (to < 100) s"00$to" else if (to < 1000) s"0$to" else if (to > 2400) "2400" else s"$to"
    val url = s"/plan/location/trains/detailed/$loc?year=$year&month=$m&day=$d&from=$f&to=$t"
    url
  }

  def createUrlForReadingTrainTimetable(train: String) = s"http://railweb-timetables-java.herokuapp.com/timetables/train/$train"

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
