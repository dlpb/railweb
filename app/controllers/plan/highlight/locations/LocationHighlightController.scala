package controllers.plan.highlight.locations

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.location.LocationService

class LocationHighlightController @Inject()(
                                             cc: ControllerComponents,
                                             authenticatedUserAction: AuthorizedWebAction,
                                             locationsService: LocationService,
                                             jwtService: JWTService

                              ) extends AbstractController(cc) with I18nSupport {

  def index(locations: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(PlanUser)){
      val token = jwtService.createToken(request.user, new Date())
      val locIds: List[String] = locations
        .replaceAll("\\s+", ",")
        .split(",")
        .toList
        .flatMap { l => locationsService.findFirstLocationByTiploc(l)}
        .map { _.id }
      Ok(views.html.plan.location.highlight.index(request.user, token, locIds, List("Work In Progress - Plan - Highlight Locations"))(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}

