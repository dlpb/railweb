package controllers.plan.timetable.train.simple

import java.util.Date

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
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, TimeoutException}

class SimpleTrainTimetableController @Inject()(
                                                cc: ControllerComponents,
                                                authenticatedUserAction: AuthorizedWebAction,
                                                locationsService: LocationsService,
                                                pathService: PathService,
                                                trainService: LocationTimetableService,
                                                timetableService: TrainTimetableService,
                                                jwtService: JWTService

                              ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(train: String, year: Int, month: Int, day: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val eventualResult = timetableService.showSimpleTrainTimetable(train, year, month, day) map {
        data =>
          if(data.isDefined) {
            Ok(views.html.plan.timetable.simple.index(request.user, token, data.get.dst, data.get.basicSchedule ,data.get.mapLocations, data.get.routes, data.get.routeLink, List.empty)(request.request))
          }
          else NotFound(views.html.plan.search.index(request.user,  locationsService.getLocations, TimetableHelper.defaultDate,
            List(s"Could not fnd train $train on $year-$month-$day. Please try searching again"
            )))
      }
      try {
        Await.result(eventualResult, Duration(30, "second"))
      }
      catch{
        case e: TimeoutException =>
          InternalServerError(views.html.plan.search.index(request.user,  locationsService.getLocations, TimetableHelper.defaultDate,
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

