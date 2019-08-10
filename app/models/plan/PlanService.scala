package models.plan

import java.io.{FileNotFoundException, InputStream}
import java.net.URL
import java.time.ZonedDateTime
import java.util.zip.GZIPInputStream

import javax.inject.Inject
import models.list.PathService
import models.location.{LocationsService, MapLocation}
import models.route.{MapRoute, Route}
import models.timetable._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source

class PlanService @Inject()(locationsService: LocationsService, pathService: PathService, reader: Reader = new WebZipInputStream) {
  def showDetailedTrainTimetable(train: String, year: Int, month: Int, day: Int) = {
    getTrain(train) map {
      tt =>
        val mapLocations = createDetailedMapLocations(tt)
        val mapRoutes = createSimpleMapRoutes(tt)
        val dst = new DisplayTimetable(locationsService, this).displayDetailedIndividualTimetable(tt, year, month, day)
        DetailedIndividualTimetable(dst, mapLocations, mapRoutes)
    }
  }

  def createSimpleMapRoutes(tt: IndividualTimetable): List[MapRoute] = {
    pathService.findRouteForWaypoints(tt.locations
      .flatMap{ l => locationsService.findLocation(l.tiploc)}
      .map(_.id))
      .routes
      .map(r => MapRoute(r))
  }

  def createSimpleMapLocations(tt: IndividualTimetable): List[MapLocation] = {
    tt.locations
        .filter(l => l.publicArrival.isDefined  || l.publicDeparture.isDefined )
        .flatMap(l => locationsService.findLocation(l.tiploc))
        .map(l => MapLocation(l))
  }

  def createDetailedMapLocations(tt: IndividualTimetable): List[MapLocation] = {
    tt.locations
        .flatMap(l => locationsService.findLocation(l.tiploc))
        .map(l => MapLocation(l))
  }


  def getTrain(train: String): Option[IndividualTimetable] = {
    implicit val formats = DefaultFormats ++ JsonFormats.allFormats

    try {
      val f = Future {
        val url = PlanService.createUrlForReadingTrainTimetable(train)

        val zis = reader.getInputStream(url)

        val string = Source.fromInputStream(zis).mkString
        Some(parse(string).extract[IndividualTimetable])
      }(scala.concurrent.ExecutionContext.Implicits.global).recoverWith {
        case e =>
          println(s"Getting Individual Timetable error = ${e.getMessage}")
          Future.successful(None)
      }(scala.concurrent.ExecutionContext.Implicits.global)
      Await.result(f, Duration(20, "second"))
    }
    catch {
      case f: FileNotFoundException => println(s"No timetable for location $train")
        None
      case e: Exception => println(s"Something went wrong: ${e.getMessage}")
        None
    }
  }

  def getTrainsForLocationAroundNow(loc: String): (List[SimpleTimetable],(Int, Int, Int, Int, Int)) = {
    val from = PlanService.from
    val to = PlanService.to

    (getTrainsForLocation(loc,
      from.getYear,
      from.getMonthValue,
      from.getDayOfMonth,
      from.getHour*100 + from.getMinute,
      to.getHour*100 + to.getMinute
    ), (from.getYear, from.getMonthValue, from.getDayOfMonth, from.getHour*100 + from.getMinute, to.getHour*100 + to.getMinute))
  }

  def getDetailedTrainsForLocationAroundNow(loc: String): (List[SimpleTimetable],(Int, Int, Int, Int, Int)) = {
    val from = PlanService.from
    val to = PlanService.to

    (getDetailedTrainsForLocation(loc,
      from.getYear,
      from.getMonthValue,
      from.getDayOfMonth,
      from.getHour*100 + from.getMinute,
      to.getHour*100 + to.getMinute
    ), (from.getYear, from.getMonthValue, from.getDayOfMonth, from.getHour*100 + from.getMinute, to.getHour*100 + to.getMinute))
  }

  def showSimpleTrainTimetable(train: String, year: Int, month: Int, day: Int): Option[SimpleIndividualTimetable] = {
    getTrain(train) map {
      tt =>
        val mapLocations = createSimpleMapLocations(tt)
        val mapRoutes = createSimpleMapRoutes(tt)
        val dst = new DisplayTimetable(locationsService, this).displayIndividualTimetable(tt, year, month, day)
        SimpleIndividualTimetable(dst, mapLocations, mapRoutes)
    }
  }

  def getTrainsForLocation(loc: String,
                           year: Int,
                           month: Int,
                           day: Int,
                           from: Int,
                           to: Int
                          ): List[SimpleTimetable] = {
    readTimetable(loc, year, month, day, from, to).toList
      .filter(PlanService.isPassengerTrain)
      .filter(PlanService.isNotPass)
  }

  def getDetailedTrainsForLocation(loc: String,
                           year: Int,
                           month: Int,
                           day: Int,
                           from: Int,
                           to: Int
                          ): List[SimpleTimetable] = {
    readTimetable(loc, year, month, day, from, to).toList
  }

  private def readTimetable(loc: String,
                            year: Int,
                            month: Int,
                            day: Int,
                            from: Int,
                            to: Int
                           ): Seq[SimpleTimetable] = {
    implicit val formats = DefaultFormats ++ JsonFormats.allFormats

    try {
      val f = Future {
        val url = PlanService.createUrlForReadingLocationTimetables(loc, year, month, day, from, to)
        val zis = reader.getInputStream(url)

        val string = Source.fromInputStream(zis).mkString
        parse(string).extract[Seq[SimpleTimetable]]
      }(scala.concurrent.ExecutionContext.Implicits.global).recoverWith {
        case e =>
          println(s"Getting Simple Timetable error = ${e.getMessage}")
          Future.successful(Seq.empty)
      }(scala.concurrent.ExecutionContext.Implicits.global)
      Await.result(f, Duration(20, "second"))

    }
    catch {
      case f: FileNotFoundException => println(s"No timetable for location $loc")
        Seq.empty
      case e: Exception => println(s"Something went wrong: ${e.getMessage}")
        Seq.empty
    }
  }
}

case class SimpleIndividualTimetable(dst: DisplaySimpleIndividualTimetable, mapLocations: List[MapLocation], routes: List[MapRoute])
case class DetailedIndividualTimetable(dtt: DisplayDetailedIndividualTimetable, mapLocations: List[MapLocation], routes: List[MapRoute])

class WebZipInputStream extends Reader {
  override def getInputStream(url: String): InputStream = {
    val is = new URL(url).openStream()
    new GZIPInputStream(is)
  }
}

trait Reader {
  def getInputStream(url: String): InputStream
}
object PlanService {

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
    val url = s"http://railweb-timetables.herokuapp.com/timetables/location/$loc?year=$year&month=$m&day=$d&from=$f&to=$t"
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

  def createUrlForDisplayingSimpleTrainTimetable(train: String, year: Int, month: Int, day: Int) = s"/plan/train/simple/$train/$year/$month/$day"
  def createUrlForDisplayingDetailedTrainTimetable(train: String, year: Int, month: Int, day: Int) = s"/plan/train/detailed/$train/$year/$month/$day"

  def createUrlForReadingTrainTimetable(train: String) = s"http://railweb-timetables.herokuapp.com/timetables/train/$train"

  def isNotPass(tt: SimpleTimetable): Boolean = tt.location.pass.isEmpty

  def isPassengerTrain(tt: SimpleTimetable): Boolean = {
    val category = tt.basicSchedule.trainCategory
    isPublicCategory(category)
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
