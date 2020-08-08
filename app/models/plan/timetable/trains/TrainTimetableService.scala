package models.plan.timetable.trains

import java.io.FileNotFoundException
import java.time.ZonedDateTime

import javax.inject.{Inject, Singleton}
import models.location.{LocationsService, MapLocation}
import models.plan.route.pointtopoint.PathService
import models.plan.timetable.reader.{Reader, WebZipInputStream}
import models.route.MapRoute
import models.timetable.dto.train.detailed.DisplayDetailedTrainTimetable
import models.timetable.dto.train.simple.DisplaySimpleTrainTimetable
import models.timetable.model.JsonFormats
import models.timetable.model.train._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

@Singleton
class TrainTimetableService @Inject()(locationsService: LocationsService, pathService: PathService, ws: WSClient, reader: Reader = new WebZipInputStream) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def showSimpleTrainTimetable(train: String, year: Int, month: Int, day: Int) = {
    getTrain(train, year.toString, month.toString, day.toString) map {
      f =>
        f map {
          tt =>
            val mapLocations = List()
            val mapRoutes = List()
            val link = TrainTimetableServiceUrlHelper.buildRouteLink(tt, locationsService)
            val dst = DisplaySimpleTrainTimetable(locationsService, tt, year, month, day)
            SimpleTrainTimetableWrapper(dst, tt.basicSchedule, mapLocations, mapRoutes, link)
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
            val link = TrainTimetableServiceUrlHelper.buildRouteLink(tt, locationsService)
            val ddt = DisplayDetailedTrainTimetable(locationsService, tt, year, month, day)
            DetailedTrainTimetableWrapper(ddt, tt.basicSchedule, mapLocations, mapRoutes, link)
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
      val url = TrainTimetableServiceUrlHelper.createUrlForReadingTrainTimetable(train, year, month, day)
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

case class SimpleTrainTimetableWrapper(dst: DisplaySimpleTrainTimetable, basicSchedule: BasicSchedule, mapLocations: List[MapLocation], routes: List[MapRoute], routeLink: String)

case class DetailedTrainTimetableWrapper(dtt: DisplayDetailedTrainTimetable, basicSchedule: BasicSchedule, mapLocations: List[MapLocation], routes: List[MapRoute], routeLink: String)

