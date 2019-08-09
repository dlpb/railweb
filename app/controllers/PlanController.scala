package controllers

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.list.{Path, PathService}
import models.location.{Location, LocationsService, MapLocation}
import models.plan.PlanService
import models.route.MapRoute
import models.timetable.{DisplayTimetable, IndividualTimetable}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

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
          new DisplayTimetable(locationsService, planService).displaySimpleTimetable(t, dates._1, dates._2, dates._3)
      }

      val l = locationsService.findLocation(loc)

      Ok(views.html.plan.location.trains.simple.index(request.user, timetables, l,
        dates._1, dates._2, dates._3, DisplayTimetable.time(dates._4), DisplayTimetable.time(dates._5))(request.request))
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
          new DisplayTimetable(locationsService, planService).displaySimpleTimetable(t, year, month, day)
      }

      val l = locationsService.findLocation(loc)

      Ok(views.html.plan.location.trains.simple.index(
        request.user, timetables, l, year, month, day,  DisplayTimetable.time(from), DisplayTimetable.time(to))(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showDetailedTrainsForLocationNow(loc: String)= authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val (timetable, dates) = planService.getDetailedTrainsForLocationAroundNow(loc)
      val timetables = timetable map {
        t =>
          new DisplayTimetable(locationsService, planService).displayDetailedTimetable(t, dates._1, dates._2, dates._3)
      }

      val l = locationsService.findLocation(loc)

      Ok(views.html.plan.location.trains.detailed.index(request.user, timetables, l,
        dates._1, dates._2, dates._3, DisplayTimetable.time(dates._4), DisplayTimetable.time(dates._5))(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showDetailedTrainsForLocation(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int, date: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val timetables = planService.getDetailedTrainsForLocation(loc, year, month, day, from, to) map {
        t =>
          new DisplayTimetable(locationsService, planService).displayDetailedTimetable(t, year, month, day)
      }

      val l = locationsService.findLocation(loc)

      Ok(views.html.plan.location.trains.detailed.index(
        request.user, timetables, l, year, month, day,  DisplayTimetable.time(from), DisplayTimetable.time(to))(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showTrain(train: String,  year: Int, month: Int, day: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val data = planService.showSimpleTrainTimetable(train, year, month, day)

      if(data.isDefined) {
        Ok(views.html.plan.train.simple.index(request.user, data.get.dst, data.get.mapLocations, data.get.routes)(request.request))
      }
      else NotFound(s"Could not find train $train on $year-$month-$day")

    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
case class DisplayIndividualTimetable(timetable: IndividualTimetable, tiplocToLocation: Map[String, Option[Location]], urls: Map[String, String])
