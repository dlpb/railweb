package controllers.plan.location

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
import models.timetable.dto.location.detailed.DisplayDetailedLocationTrain
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

class DetailedLocationTrainController @Inject()(
                                                 cc: ControllerComponents,
                                                 authenticatedUserAction: AuthorizedWebAction,
                                                 locationsService: LocationsService,
                                                 pathService: PathService,
                                                 trainService: LocationTrainService,
                                                 timetableService: TimetableService,
                                                 jwtService: JWTService

                                               ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global


  def showDetailedTrainsForLocation(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int, date: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val locations = locationsService.findAllLocationsMatchingCrs(loc)
      println(s"Showing results for ${locations.map(_.tiploc)}")

      if (locations.nonEmpty) {
        val allTiplocResults = locations.map(location => trainService.getDetailedTrainsForLocation(location.id, year, month, day, from, to))
        val allTimetables: Set[Future[Seq[DisplayDetailedLocationTrain]]] = allTiplocResults.map(result => result.timetables map {
          f =>
            f map {
              t =>
                DisplayDetailedLocationTrain(locationsService, t, result.year, result.month, result.day)
            }
        })

        val timetables = Future.sequence(allTimetables)

        val l = locations.head
        val eventualResult: Future[Result] = timetables map {
          timetable: Set[Seq[DisplayDetailedLocationTrain]] =>
            val t = timetable.flatten
            val result = allTiplocResults.head
            val locationName = l.name
            val locationId = if(l.crs.nonEmpty && l.isOrrStation) l.crs.head else l.id
            Ok(views.html.plan.location.trains.detailed.index(
              request.user, t.toList, locationName, locationId, result.year, result.month, result.day, TimetableHelper.time(result.from), TimetableHelper.time(result.to), locationsService.getLocations, List.empty)(request.request))
        }
        try {
          Await.result(eventualResult, Duration(30, "second"))
        }
        catch {
          case e: TimeoutException =>
            InternalServerError(views.html.plan.search.index(request.user, locationsService.getLocations,defaultDate,
              List(s"Could not get details for $loc on $year-$month-$day",
                "Timed out producing the page"
              ))
            (request.request))
        }
      }
      else {
        NotFound(views.html.plan.search.index(request.user, locationsService.getLocations,defaultDate,
          List(s"Could not get details for $loc on $year-$month-$day",
            s"Could not find location ${loc}"
          ))
        (request.request))
      }
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


}

