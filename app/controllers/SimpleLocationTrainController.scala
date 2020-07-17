package controllers

import java.time.ZonedDateTime
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.list.PathService
import models.location.LocationsService
import models.plan.timetable.TimetableService
import models.plan.trains.LocationTrainService
import models.timetable.dto.TimetableHelper
import models.timetable.dto.location.simple.DisplaySimpleLocationTrain
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

class SimpleLocationTrainController @Inject()(
                                               cc: ControllerComponents,
                                               authenticatedUserAction: AuthorizedWebAction,
                                               locationsService: LocationsService,
                                               pathService: PathService,

                                               locationTrainService: LocationTrainService,
                                               timetableService: TimetableService,
                                               jwtService: JWTService

                                             ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def showTrainsForLocationNow(loc: String): Action[AnyContent] = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {

      val token = jwtService.createToken(request.user, new Date())

      val location = locationsService.findLocation(loc)
      if (location.isDefined) {

        val (timetable, dates) = locationTrainService.getTrainsForLocationAroundNow(loc)
        val timetables = timetable map {
          f =>
            f.filter(tt => tt.publicStop && tt.publicTrain) map {
              t =>
                DisplaySimpleLocationTrain(locationsService, t, dates._1, dates._2, dates._3)
            }
        }

        val eventualResult: Future[Result] = timetables map {
          t =>
            Ok(views.html.plan.location.trains.simple.index(request.user, t.toList, location,
              dates._1, dates._2, dates._3, TimetableHelper.time(dates._4), TimetableHelper.time(dates._5), locationsService.getLocations, List.empty)(request.request))
        }
        try {
          Await.result(eventualResult, Duration(30, "second"))
        }
        catch {
          case e: TimeoutException =>
            InternalServerError(views.html.plan.search.index(request.user, locationsService.getLocations, defaultDate,
              List(s"Could not get details for $loc around now",
                "Timed out producing the page"
              ))
            (request.request))
        }
      }
      else {
        NotFound(views.html.plan.search.index(request.user, locationsService.getLocations, defaultDate,
          List(s"Could not get details for location $loc around now",
            s"Location ${loc} not found"
          ))(request.request))
      }
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showTrainSearchIndex(): Action[AnyContent] = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {

      Ok(views.html.plan.search.index(request.user, locationsService.getLocations, defaultDate, List())(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }


  private def defaultDate = {
    val now = ZonedDateTime.now
    val defaultDate = s"${now.getYear}-${now.getMonthValue}-${now.getDayOfMonth}"
    defaultDate
  }

  def showTrainsForLocation(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int, date: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val (y, m, d): (Int, Int, Int) = if (date.contains("-")) {
        val dateParts = date.split("-").map(_.toInt)
        (dateParts(0), dateParts(1), dateParts(2))
      } else (year, month, day)

      val location = locationsService.findLocation(loc)
      if (location.isDefined) {
        val timetables = locationTrainService.getTrainsForLocation(location.get.id, y, m, d, from, to) map {
          f =>
            f.filter(tt => tt.publicStop && tt.publicTrain) map {
              t =>
                DisplaySimpleLocationTrain(locationsService, t, y, m, d)
            }
        }

        val l = locationsService.findLocation(loc)
        val eventualResult: Future[Result] = timetables map {
          t =>
            Ok(views.html.plan.location.trains.simple.index(
              request.user, t.toList, l, y, m, d, TimetableHelper.time(from), TimetableHelper.time(to), locationsService.getLocations,  List.empty)(request.request))
        }
        try {
          Await.result(eventualResult, Duration(30, "second"))
        }
        catch {
          case e: TimeoutException =>
            InternalServerError(views.html.plan.search.index(request.user, locationsService.getLocations, defaultDate,
              List(s"Could not get details for location $loc on $y-$m-$d",
                "Timed out producing the page"
              ))
            (request.request))
        }
      }
      else {
        NotFound(views.html.plan.search.index(request.user, locationsService.getLocations, defaultDate,
          List(s"Could not get details for location $loc on $y-$m-$d",
            s"Location ${loc} not found"
          ))(request.request))
      }
    }

    else {
      Forbidden("User not authorized to view page")
    }
  }

}

