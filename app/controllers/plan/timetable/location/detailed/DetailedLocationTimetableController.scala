package controllers.plan.timetable.location.detailed

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import controllers.plan.timetable.location.simple.SimpleTimeShift
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.location.LocationsService
import models.plan.route.pointtopoint.PointToPointRouteFinderService
import models.plan.timetable.TimetableDateTimeHelper
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.dto.TimetableHelper
import models.timetable.dto.location.detailed.DisplayDetailedLocationTimetable
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

class DetailedLocationTimetableController @Inject()(
                                                     cc: ControllerComponents,
                                                     authenticatedUserAction: AuthorizedWebAction,
                                                     locationsService: LocationsService,
                                                     pathService: PointToPointRouteFinderService,
                                                     trainService: LocationTimetableService,
                                                     timetableService: TrainTimetableService,
                                                     jwtService: JWTService

                                               ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global


  def index(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int, date: String, hasCalledAt: String, willCallAt: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val dateMessage = if(from > to) Some("The selected time range spans midnight. Only trains up until midnight on the date selected will be shown.") else None

      println(s"Fetching Detailed trains for $loc on $year-$month-$day (date $date) from $from to $to having called at $hasCalledAt and will call at $willCallAt")

      val hca = if(!hasCalledAt.isBlank) locationsService.findLocationByNameTiplocCrsOrId(hasCalledAt).map(_.id) else None
      val wca = if(!willCallAt.isBlank) locationsService.findLocationByNameTiplocCrsOrId(willCallAt).map(_.id) else None

      val (actualDate, actualYear, actualMonth, actualDay) = if(date.isBlank) {
        if(year == 0 || month == 0 || day == 0)
          (LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalDate.now.getYear, LocalDate.now.getMonthValue, LocalDate.now.getDayOfMonth)
        else
          (s"$year-$month-$day", year, month, day)
      } else {
        val actualDateSupplied = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        (date, actualDateSupplied.getYear, actualDateSupplied.getMonthValue, actualDateSupplied.getDayOfMonth)
      }

      val result = trainService.getDetailedTimetablesForLocation(loc, actualYear, actualMonth, actualDay, from, to, actualDate, hca, wca)

      val now = s"$actualYear-$actualMonth-$actualDay ${TimetableDateTimeHelper.padTime(result.from)}"
      val requestedDateTime =
        LocalDateTime.parse(
          now,
          DateTimeFormatter.ofPattern("yyyy-M-d HHmm"))

      val oneHourLaterTime = requestedDateTime.plusHours(1)
      val oneDayLaterTime = requestedDateTime.plusDays(1)
      val oneDayEarlierTime = requestedDateTime.minusDays(1)
      val oneHourEarlierTime = requestedDateTime.minusHours(1)

      val oneHourEarlier = SimpleTimeShift(oneHourEarlierTime)
      val oneDayEarlier = SimpleTimeShift(oneDayEarlierTime)
      val oneDayLater = SimpleTimeShift(oneDayLaterTime)
      val oneHourLater = SimpleTimeShift(oneHourLaterTime)

      if(result.locations.nonEmpty) {

        val l = result.locations.head
        val eventualResult: Future[Result] = result.timetables map {
          timetable: Seq[DisplayDetailedLocationTimetable] =>
            val locationName = l.name
            val locationId = if(l.crs.nonEmpty && l.isOrrStation) l.crs.head else l.id
            Ok(views.html.plan.location.trains.detailed.index(
              request.user,
              timetable.toList,
              locationName,
              locationId,
              result.year,
              result.month,
              result.day,
              TimetableHelper.time(result.from),
              TimetableHelper.time(result.to),
              hasCalledAt,
              willCallAt,
              locationsService.getLocations,
              oneHourEarlier,
              oneDayEarlier,
              oneDayLater,
              oneHourLater,
              dateMessage.toList)(request.request))
        }
        try {
          Await.result(eventualResult, Duration(30, "second"))
        }
        catch {
          case e: TimeoutException =>
            InternalServerError(views.html.plan.search.index(request.user, locationsService.getLocations, TimetableHelper.defaultDate,
              List(s"Could not get details for $loc on $year-$month-$day",
                "Timed out producing the page"
              ))
            (request.request))
        }
      }
      else {
        NotFound(views.html.plan.search.index(request.user, locationsService.getLocations, TimetableHelper.defaultDate,
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

}

