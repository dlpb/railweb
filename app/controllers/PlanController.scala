package controllers

import java.io
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.list.{Path, PathService}
import models.location.{Location, LocationsService, MapLocation}
import models.plan.PlanService
import models.route.MapRoute
import models.timetable.{DisplaySimpleTimetable, DisplayTimetable, IndividualTimetable}
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.duration.Duration

class PlanController @Inject()(
                                cc: ControllerComponents,
                                authenticatedUserAction: AuthorizedWebAction,
                                locationsService: LocationsService,
                                pathService: PathService,
                                planService: PlanService,
                                jwtService: JWTService

                              ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def showLocationHighlightsForTrains(trainsAndStations: String, srsLocations: String, locations: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())
      val mapLocations: List[MapLocation] =
        if(locations.isEmpty) { List.empty[MapLocation] }
        else locations
        .replaceAll("\\s+", ",")
        .split(",")
        .flatMap {
          locationsService.getLocation
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

      println(s"Map Location Size ${mapLocations.size}")
      println(s"SRS Location Size ${srsMapLocations.size}")

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
            val tt: Future[Option[IndividualTimetable]] = planService.getTrain(train)
            val result: Future[List[MapLocation]] = tt.map {
              _.map {
                t: IndividualTimetable =>
                  println(s"got timetable for $train")
                  val locs: List[Location] = t.locations.filter {
                    l =>
                      l.pass.isEmpty && (l.publicDeparture.isDefined || l.publicArrival.isDefined)
                  }.map {
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
  def showPlanIndex(): Action[AnyContent] = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      Ok(views.html.plan.index(request.user)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showTrainsForLocationNow(loc: String): Action[AnyContent] = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val (timetable, dates) = planService.getTrainsForLocationAroundNow(loc)
      val timetables: Future[Seq[DisplaySimpleTimetable]] = timetable map {
        f =>
          f map {
            t =>
              new DisplayTimetable(locationsService, planService).displaySimpleTimetable(t, dates._1, dates._2, dates._3)
          }
      }

      val l = locationsService.findLocation(loc)

      val eventualResult: Future[Result] = timetables map {
        t =>
          Ok(views.html.plan.location.trains.simple.index(request.user, t.toList, l,
            dates._1, dates._2, dates._3, DisplayTimetable.time(dates._4), DisplayTimetable.time(dates._5))(request.request))
      }
      try {
        Await.result(eventualResult, Duration(30, "second"))
      }
      catch{
        case e: TimeoutException =>
          InternalServerError(views.html.plan.error.index(request.user,
            List(s"Could not get details for $loc around now",
              "Timed out producing the page"
            ))
          (request.request))
      }
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showTrainsForLocation(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int, date: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val timetables = planService.getTrainsForLocation(loc, year, month, day, from, to) map {
        f =>
          f map {
            t =>
              new DisplayTimetable(locationsService, planService).displaySimpleTimetable(t, year, month, day)
          }
      }

      val l = locationsService.findLocation(loc)
      val eventualResult: Future[Result] = timetables map {
        t =>
          Ok(views.html.plan.location.trains.simple.index(
            request.user, t.toList, l, year, month, day,  DisplayTimetable.time(from), DisplayTimetable.time(to))(request.request))
      }
      try {
        Await.result(eventualResult, Duration(30, "second"))
      }
      catch{
        case e: TimeoutException =>
          InternalServerError(views.html.plan.error.index(request.user,
            List(s"Could not get details for location $loc on $year-$month-$day",
              "Timed out producing the page"
            ))
          (request.request))
      }    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showDetailedTrainsForLocationNow(loc: String)= authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val (timetable, dates) = planService.getDetailedTrainsForLocationAroundNow(loc)
      val timetables = timetable map {
        f =>
          f map {
            t =>
              new DisplayTimetable(locationsService, planService).displayDetailedTimetable(t, dates._1, dates._2, dates._3)
          }
      }

      val l = locationsService.findLocation(loc)
      val eventualResult: Future[Result] = timetables map {
        t =>
          Ok(views.html.plan.location.trains.detailed.index(request.user, t.toList, l,
            dates._1, dates._2, dates._3, DisplayTimetable.time(dates._4), DisplayTimetable.time(dates._5))(request.request))
      }
      try {
        Await.result(eventualResult, Duration(30, "second"))
      }
      catch{
        case e: TimeoutException =>
          InternalServerError(views.html.plan.error.index(request.user,
            List(s"Could not get details for train $loc around now",
              "Timed out producing the page"
            ))
          (request.request))
      }    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showDetailedTrainsForLocation(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int, date: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val timetables = planService.getDetailedTrainsForLocation(loc, year, month, day, from, to) map {
        f =>
          f map {
            t =>
              new DisplayTimetable(locationsService, planService).displayDetailedTimetable(t, year, month, day)
          }
      }

      val l = locationsService.findLocation(loc)
      val eventualResult: Future[Result] = timetables map {
        t =>
          Ok(views.html.plan.location.trains.detailed.index(
            request.user, t.toList, l, year, month, day, DisplayTimetable.time(from), DisplayTimetable.time(to))(request.request))
      }
      try {
        Await.result(eventualResult, Duration(30, "second"))
      }
      catch{
        case e: TimeoutException =>
          InternalServerError(views.html.plan.error.index(request.user,
            List(s"Could not get details for $loc on $year-$month-$day",
              "Timed out producing the page"
            ))
          (request.request))
      }
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showTrain(train: String,  year: Int, month: Int, day: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val eventualResult = planService.showSimpleTrainTimetable(train, year, month, day) map {
        data =>
          if(data.isDefined) {
            Ok(views.html.plan.train.simple.index(request.user, token, data.get.dst, data.get.mapLocations, data.get.routes)(request.request))
          }
          else NotFound(views.html.plan.error.index(request.user,
            List(s"Could not fnd train $train on $year-$month-$day",
              "Go back to <a href='/plan'>Plan</a>"
            )))
      }
      try {
        Await.result(eventualResult, Duration(30, "second"))
      }
      catch{
        case e: TimeoutException =>
          InternalServerError(views.html.plan.error.index(request.user,
            List(s"Could not get details for train $train on $year-$month-$day",
              "Timed out producing the page"
            ))
          (request.request))
      }
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showDetailedTrain(train: String,  year: Int, month: Int, day: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val eventualResult = planService.showDetailedTrainTimetable(train, year, month, day) map {
        data =>
          if(data.isDefined)
            Ok(views.html.plan.train.detailed.index(request.user, token, data.get.dtt, data.get.mapLocations, data.get.routes)(request.request))
          else NotFound(views.html.plan.error.index(request.user,
            List(s"Could not fnd train $train on $year-$month-$day",
              "Go back to <a href='/plan'>Plan</a>"
            ))
          (request.request))
      }
      try {
        Await.result(eventualResult, Duration(30, "second"))
      }
      catch{
        case e: TimeoutException =>
          InternalServerError(views.html.plan.error.index(request.user,
            List(s"Could not get details for train $train on $year-$month-$day",
              "Timed out producing the page"
            ))
          (request.request))
      }

    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
case class DisplayIndividualTimetable(timetable: IndividualTimetable, tiplocToLocation: Map[String, Option[Location]], urls: Map[String, String])
