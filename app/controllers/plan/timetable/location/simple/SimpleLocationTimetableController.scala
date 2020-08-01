package controllers.plan.timetable.location.simple

import java.time.ZonedDateTime
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.list.PathService
import models.location.LocationsService
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.dto.TimetableHelper
import models.timetable.dto.location.simple.DisplaySimpleLocationTrain
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

class SimpleLocationTimetableController @Inject()(
                                                   cc: ControllerComponents,
                                                   authenticatedUserAction: AuthorizedWebAction,
                                                   locationsService: LocationsService,
                                                   pathService: PathService,
                                                   locationTrainService: LocationTimetableService,
                                                   timetableService: TrainTimetableService,
                                                   jwtService: JWTService

                                             ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int, date: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>

    println("trying train at specific time")
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val (y, m, d): (Int, Int, Int) = if (date.contains("-")) {
        val dateParts = date.split("-").map(_.toInt)
        (dateParts(0), dateParts(1), dateParts(2))
      } else (year, month, day)

      val locations = locationsService.findAllLocationsMatchingCrs(loc)

      println(s"Showing results for ${locations.map(_.tiploc)}")
      if (locations.nonEmpty) {
        val allTiplocResults = locations.map(location => locationTrainService.getTrainsForLocation(location.id, y, m, d, from, to))

        val allTimetables: Seq[Future[Seq[DisplaySimpleLocationTrain]]] = allTiplocResults.map(result => result.timetables map {
          f =>
            f.filter(tt => tt.publicStop && tt.publicTrain) map {
              t =>
                DisplaySimpleLocationTrain(locationsService, t, result.year, result.month, result.day)
            }
        }).toSeq

        val timetables = Future.sequence(allTimetables)

        val l = locations.head
        val eventualResult: Future[Result] = timetables map {
          timetable: Seq[Seq[DisplaySimpleLocationTrain]] =>
            val t: Seq[DisplaySimpleLocationTrain] = timetable.flatten
            val result = allTiplocResults.head
            val locationName = l.name
            val locationId = if(l.crs.nonEmpty && l.isOrrStation) l.crs.head else l.id
            Ok(views.html.plan.location.trains.simple.index(
              request.user, t.toList, locationName, locationId, result.year, result.month, result.day, TimetableHelper.time(result.from), TimetableHelper.time(result.to), locationsService.getLocations,  List.empty)(request.request))
        }
        try {
          Await.result(eventualResult, Duration(30, "second"))
        }
        catch {
          case e: TimeoutException =>
            InternalServerError(views.html.plan.search.index(request.user, locationsService.getLocations, TimetableHelper.defaultDate,
              List(s"Could not get details for location $loc on $y-$m-$d",
                "Timed out producing the page"
              ))
            (request.request))
        }
      }
      else {
        NotFound(views.html.plan.search.index(request.user, locationsService.getLocations, TimetableHelper.defaultDate,
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

