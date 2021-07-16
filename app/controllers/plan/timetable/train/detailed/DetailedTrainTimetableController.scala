package controllers.plan.timetable.train.detailed

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.dto.TimetableHelper
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.location.LocationService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, TimeoutException}

class DetailedTrainTimetableController @Inject()(
                                                  cc: ControllerComponents,
                                                  authenticatedUserAction: AuthorizedWebAction,
                                                  locationsService: LocationService,
                                                  timetableService: TrainTimetableService,
                                                  jwtService: JWTService

                              ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(train: String, year: Int, month: Int, day: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val eventualResult = timetableService.showDetailedTrainTimetable(train, year, month, day) map {
        data =>
          if(data.isDefined)
            Ok(views.html.plan.timetable.detailed.index(request.user, token, data.get.dtt, data.get.basicSchedule, data.get.locations, data.get.routes, data.get.routeLink, List.empty)(request.request))
          else NotFound(views.html.plan.search.index(request.user,  locationsService.locations.toList, TimetableHelper.defaultDate,
            List(s"Could not fnd train $train on $year-$month-$day. Please try searching again."
            ))
          (request.request))
      }
      try {
        Await.result(eventualResult, Duration(30, "second"))
      }
      catch{
        case e: TimeoutException =>
          InternalServerError(views.html.plan.search.index(request.user,  locationsService.locations.toList, TimetableHelper.defaultDate,
            List(s"Could not get details for train $train on $year-$month-$day. Timed out producing the page"
            ))
          (request.request))
      }

    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

}

