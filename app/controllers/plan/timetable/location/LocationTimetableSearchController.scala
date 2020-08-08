package controllers.plan.timetable.location

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.location.LocationsService
import models.plan.route.pointtopoint.PathService
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.dto.TimetableHelper
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class LocationTimetableSearchController @Inject()(
                                                   cc: ControllerComponents,
                                                   authenticatedUserAction: AuthorizedWebAction,
                                                   locationsService: LocationsService,
                                                   pathService: PathService,
                                                   locationTrainService: LocationTimetableService,
                                                   timetableService: TrainTimetableService,
                                                   jwtService: JWTService

                                                 ) extends AbstractController(cc) with I18nSupport {

  def index(): Action[AnyContent] = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {

      Ok(views.html.plan.search.index(request.user, locationsService.getLocations, TimetableHelper.defaultDate, List())(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
