package controllers

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.location.LocationsService
import models.plan.PlanService
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

  def showTrainsForLocation(loc: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val timetable = planService.getTrainsForLocation(loc)

      Ok(views.html.plan.location.trains.index(request.user, timetable)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
