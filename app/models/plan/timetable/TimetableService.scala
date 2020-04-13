package models.plan.timetable

import java.io.FileNotFoundException
import java.time.ZonedDateTime

import javax.inject.Inject
import models.list.PathService
import models.location.{LocationsService, MapLocation}
import models.plan.reader.{Reader, WebZipInputStream}
import models.route.MapRoute
import models.timetable.dto.timetable.detailed.DisplayDetailedIndividualTimetable
import models.timetable.dto.timetable.simple.DisplaySimpleIndividualTimetable
import models.timetable.model.JsonFormats
import models.timetable.model.train._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

class TimetableService @Inject()(locationsService: LocationsService, pathService: PathService, ws: WSClient, reader: Reader = new WebZipInputStream) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def showSimpleTrainTimetable(train: String, year: Int, month: Int, day: Int) = {
    getTrain(train, year.toString, month.toString, day.toString) map {
      f =>
        f map {
          tt =>
            val mapLocations = List()
            val mapRoutes = List()
            val link = TimetableService.buildRouteLink(tt, locationsService)
            val dst = DisplaySimpleIndividualTimetable(locationsService, tt, year, month, day)
            SimpleIndividualTimetableWrapper(dst, tt.basicSchedule, mapLocations, mapRoutes, link)
        }
    }
  }
  def showDetailedTrainTimetable(train: String, year: Int, month: Int, day: Int) = {
    getTrain(train, year.toString, month.toString, day.toString) map {
      f =>
        f map {
          tt =>
            val mapLocations = List()
            val mapRoutes = List()
            val link = TimetableService.buildRouteLink(tt, locationsService)
            val ddt = DisplayDetailedIndividualTimetable(locationsService, tt, year, month, day)
            DetailedIndividualTimetableWrapper(ddt, tt.basicSchedule, mapLocations, mapRoutes, link)
        }
    }
  }

  def createSimpleMapRoutes(tt: IndividualTimetable): List[MapRoute] = {
    val waypoints = tt.locations
      .flatMap { l => locationsService.findLocation(l.tiploc) }
      .map(_.id)

    val routeParts: Iterator[Future[List[MapRoute]]] = waypoints.sliding(2).map { w => Future {
      pathService.findRouteForWaypoints(w).routes.map(r => MapRoute(r))
    }}
    val eventualIterator: Future[Iterator[MapRoute]] = Future.sequence(routeParts) map {i => i.flatten}

    try {
      Await.result(eventualIterator, Duration(30, "second")).toList
    }
    catch {
      case e: TimeoutException =>
        println("Could not get waypoints")
        List()
    }
  }

  def createSimpleMapLocations(tt: IndividualTimetable): List[MapLocation] = {
    tt.locations
      .filter(l => l.publicArrival.isDefined || l.publicDeparture.isDefined)
      .flatMap(l => locationsService.findLocation(l.tiploc))
      .map(l => MapLocation(l))
  }

  def createDetailedMapLocations(tt: IndividualTimetable): List[MapLocation] = {
    tt.locations
      .flatMap(l => locationsService.findLocation(l.tiploc))
      .map(l => MapLocation(l))
  }


  def getTrain(train: String, year: String, month: String, day: String): Future[Option[IndividualTimetable]] = {
    implicit val formats = DefaultFormats ++ JsonFormats.formats

    try {
      val url = TimetableService.createUrlForReadingTrainTimetable(train, year, month, day)
      println(s"getting data for url $url")
      val request: WSRequest = ws.url(url)
      request
        .withRequestTimeout(Duration(20, "second"))
        .get()
        .map {
          response =>
            Some(parse(response.body).extract[IndividualTimetable])

        }(scala.concurrent.ExecutionContext.Implicits.global).recoverWith {
        case e =>
          println(s"Something went wrong: ${e.getMessage}")
          e.printStackTrace()
          Future.successful(None)
      }

    }
    catch {
      case f: FileNotFoundException => println(s"No timetable for location $train")
        Future.successful(None)
      case e: Exception => println(s"Something went wrong: ${e.getMessage}")
        Future.successful(None)
    }
  }
}

case class SimpleIndividualTimetableWrapper(dst: DisplaySimpleIndividualTimetable, basicSchedule: BasicSchedule, mapLocations: List[MapLocation], routes: List[MapRoute], routeLink: String)

case class DetailedIndividualTimetableWrapper(dtt: DisplayDetailedIndividualTimetable, basicSchedule: BasicSchedule, mapLocations: List[MapLocation], routes: List[MapRoute], routeLink: String)

object TimetableService {

  def from: ZonedDateTime = ZonedDateTime.now().minusMinutes(15)

  def to: ZonedDateTime = ZonedDateTime.now().plusMinutes(45)

  def hourMinute(time: Int) = {
    val hour = time / 100
    val minute = time % 100
    (hour, minute)
  }

  def createUrlForDisplayingTrainSimpleTimetable(uid: String, year: Int, month: Int, day: Int) = {
    val m = if (month < 1) "01" else if (month < 10) s"0$month" else if (month > 12) "12" else s"$month"
    val d = if (day < 1) "01" else if (day < 10) s"0$day" else if (day > 31) "31" else s"$day"
    val url = s"/plan/timetables/train/$uid/simple/$year/$m/$d"
    url
  }

  def createUrlForDisplayingDetailedTrainTimetable(uid: String, year: Int, month: Int, day: Int) = {
    val m = if (month < 1) "01" else if (month < 10) s"0$month" else if (month > 12) "12" else s"$month"
    val d = if (day < 1) "01" else if (day < 10) s"0$day" else if (day > 31) "31" else s"$day"
    val url = s"/plan/timetables/train/$uid/detailed/$year/$m/$d"
    url
  }

  def buildRouteLink(tt: IndividualTimetable, locService: LocationsService): String = {
    val ids = tt.locations.flatMap(l => locService.findLocation(l.tiploc).map(_.id)).mkString("%0D%0A")
    val url = s"/routes/find?followFixedLinks=false&followFreightLinks=true&waypoints=$ids"
    url
  }

  def createUrlForReadingTrainTimetable(train: String, year: String, month: String, day: String) = s"http://railweb-timetables-java.herokuapp.com/timetables/train/$train?year=$year&month=$month&day=$day"
//  def createUrlForReadingTrainTimetable(train: String, year: String, month: String, day: String) = s"http://localhost:9090/timetables/train/$train?year=$year&month=$month&day=$day"
}
