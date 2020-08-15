package controllers.plan.timetable.location.simple

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.location.LocationsService
import models.plan.route.pointtopoint.PointToPointRouteFinderService
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.dto.TimetableHelper
import models.timetable.dto.location.simple.DisplaySimpleLocationTimetable
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

class SimpleLocationTimetableController @Inject()(
                                                   cc: ControllerComponents,
                                                   authenticatedUserAction: AuthorizedWebAction,
                                                   locationsService: LocationsService,
                                                   pathService: PointToPointRouteFinderService,
                                                   locationTrainService: LocationTimetableService,
                                                   timetableService: TrainTimetableService,
                                                   jwtService: JWTService

                                                 ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int, date: String, hasCalledAt: String, willCallAt: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>

    println(s"Fetching Simple trains for $loc on $year-$month-$day (date $date) from $from to $to having called at $hasCalledAt and will call at $willCallAt")
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val hca = if(!hasCalledAt.isBlank) locationsService.findLocation(hasCalledAt).map(_.id) else None
      val wca = if(!willCallAt.isBlank) locationsService.findLocation(willCallAt).map(_.id) else None

      val result = locationTrainService.getSimpleTimetablesForLocation(loc, year, month, day, from, to, date, hca, wca)

      if(result.locations.nonEmpty) {
        val l = result.locations.head
        val eventualResult: Future[Result] = result.timetables map {
          timetable: Seq[DisplaySimpleLocationTimetable] =>
            val locationName = l.name
            val locationId = if (l.crs.nonEmpty && l.isOrrStation) l.crs.head else l.id
            Ok(views.html.plan.location.trains.simple.index(
              request.user, timetable.toList, locationName, locationId, result.year, result.month, result.day, TimetableHelper.time(result.from), TimetableHelper.time(result.to), hasCalledAt, willCallAt, locationsService.getLocations, List.empty)(request.request))
        }
        try {
          Await.result(eventualResult, Duration(30, "second"))
        }
        catch {
          case e: TimeoutException =>
            InternalServerError(views.html.plan.search.index(request.user, locationsService.getLocations, TimetableHelper.defaultDate,
              List(s"Could not get details for location $loc on ${result.year}-${result.month}-${result.day}",
                "Timed out producing the page"
              ))
            (request.request))
        }
      }
      else
      {
        NotFound(views.html.plan.search.index(request.user, locationsService.getLocations, TimetableHelper.defaultDate,
          List(s"Could not get details for location $loc on ${year}-${month}-${day}",
            s"Location ${loc} not found"
          ))(request.request))
      }
    }

    else {
      Forbidden("User not authorized to view page")
    }
  }

}

