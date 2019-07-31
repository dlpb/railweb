package controllers

import java.time.{LocalDateTime, ZonedDateTime}
import java.time.temporal.TemporalUnit
import java.util.{Calendar, Date}

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.location.{Location, LocationsService}
import models.plan.PlanService
import models.timetable.SimpleTimetable
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

class PlanController @Inject()(
                                cc: ControllerComponents,
                                authenticatedUserAction: AuthorizedWebAction,
                                locationsService: LocationsService,
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

  def showTrainsForLocationNow(loc: String) = {

    val from: ZonedDateTime = ZonedDateTime.now().minusMinutes(15)

    val to: ZonedDateTime = ZonedDateTime.now().plusMinutes(45)

    showTrainsForLocation(loc,
      from.getYear,
      from.getMonthValue,
      from.getDayOfMonth,
      from.getHour*100 + from.getMinute,
      to.getHour*100 + to.getMinute
      )
  }

  def showTrainsForLocation(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val timetables = planService.getTrainsForLocation(loc, year, month, day, from, to) map {
        t =>
          DisplaySimpleTimetable(t,
            locationsService.findLocation(t.origin.tiploc),
            locationsService.findLocation(t.location.tiploc),
            locationsService.findLocation(t.destination.tiploc))
      }

      val l = locationsService.findLocation(loc)

      Ok(views.html.plan.location.trains.index(request.user, timetables, l, year, month, day, from, to)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}

case class DisplaySimpleTimetable(timetable: SimpleTimetable, origin: Option[Location], location: Option[Location], destination: Option[Location])
