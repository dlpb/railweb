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
