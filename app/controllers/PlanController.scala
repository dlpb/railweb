package controllers

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId, ZonedDateTime}
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.location.LocationsService
import models.list.{Path, PathService}
import models.location.{Location, LocationsService, MapLocation}
import models.plan.PlanService
import models.timetable.DisplayTimetable
import models.route.{MapRoute, Route}
import models.timetable
import models.timetable.{IndividualTimetable, SimpleTimetable}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.collection.immutable

class PlanController @Inject()(
                                cc: ControllerComponents,
                                authenticatedUserAction: AuthorizedWebAction,
                                locationsService: LocationsService,
                                pathService: PathService,
                                planService: PlanService,
                                jwtService: JWTService

                              ) extends AbstractController(cc) with I18nSupport {
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
  def showPlanIndex() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      Ok(views.html.plan.index(request.user)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showTrainsForLocationNow(loc: String)= authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val (timetable, dates) = planService.getTrainsForLocationAroundNow(loc)
      val timetables = timetable map {
        t =>
          new DisplayTimetable(locationsService)(t)
      }

      val l = locationsService.findLocation(loc)

      Ok(views.html.plan.location.trains.index(request.user, timetables, l, dates._1, dates._2, dates._3, dates._4, dates._5)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showTrainsForLocation(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int, date: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val timetables = planService.getTrainsForLocation(loc, year, month, day, from, to) map {
        t =>
          new DisplayTimetable(locationsService)(t)
      }

      val l = locationsService.findLocation(loc)

      Ok(views.html.plan.location.trains.index(
        request.user, timetables, l, year, month, day, from, to)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def timetablesForLocation(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int) = {
    planService.getTrainsForLocation(loc, year, month, day, from, to)
  }

  def showTrain(train: String,  year: Int, month: Int, day: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

        val timetable = planService.getTrain(train) map {
        t =>
          val ttl = t.locations map { l =>
              l.tiploc -> locationsService.findLocation(l.tiploc)
          }

          val urls = t.locations map { l =>
            def hourMinute(time: Int) = {
              val hour = time / 100
              val minute = time % 100
              (hour, minute)
            }

            val (hour, minute) = if(l.pass.isDefined) hourMinute(l.pass.get)
            else if (l.arrival.isDefined) hourMinute(l.arrival.get)
            else if (l.departure.isDefined) hourMinute(l.departure.get)
            else (0,0)

            val dateFrom = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.systemDefault()).minusMinutes(15)
            val dateTo = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.systemDefault()).plusMinutes(45)


            l.tiploc -> s"""/plan/location/trains/simple?loc=${locationsService.findLocation(l.tiploc).map(_.id).getOrElse("")}
              |&year=${dateFrom.getYear}
              |&month=${dateFrom.getMonthValue}
              |&day=${dateFrom.getDayOfMonth}
              |&from=${dateFrom.getHour}${dateFrom.getMinute}
              |&to=${dateTo.getHour}${dateTo.getMinute}""".stripMargin
          }

          DisplayIndividualTimetable(t, ttl.toMap, urls.toMap)
      }


      val ids = timetable
          .map(_.timetable)
          .map(_.locations)
          .getOrElse(List.empty)
          .flatMap(l => locationsService.findLocation(l.tiploc))
          .map{_.id}


       val path: Path = pathService.findRouteForWaypoints(ids)

      val distance = path.routes
        .map {_.distance}
        .sum
      val mapRoutes: List[MapRoute] = path.routes map { MapRoute(_) }
      val mapLocations: List[MapLocation] = path.locations map { MapLocation(_) }

      Ok(views.html.plan.train.index(request.user, timetable, mapLocations, mapRoutes, distance)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
case class DisplayIndividualTimetable(timetable: IndividualTimetable, tiplocToLocation: Map[String, Option[Location]], urls: Map[String, String])
