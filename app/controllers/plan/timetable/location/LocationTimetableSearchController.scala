package controllers.plan.timetable.location

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.timetable.dto.TimetableHelper
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.location.LocationService

class LocationTimetableSearchController @Inject()(
                                                   cc: ControllerComponents,
                                                   authenticatedUserAction: AuthorizedWebAction,
                                                   locationsService: LocationService,
                                                   jwtService: JWTService

                                                 ) extends AbstractController(cc) with I18nSupport {

  def index(): Action[AnyContent] = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {

      Ok(views.html.plan.search.index(request.user, locationsService.locations.toList, TimetableHelper.defaultDate, List())(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
