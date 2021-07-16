package controllers.plan.route.find.result.timetable

import java.net.URLDecoder
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.Date
import java.util.concurrent.TimeUnit

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import controllers.plan.route.find.result.FindRouteResultHelper.mkTime
import controllers.plan.route.find.result.{FindRouteResultHelper, ResultViewModel, Waypoint}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.{Location}
import models.plan.timetable.trains.TrainTimetableService
import models.route.Route
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.location.LocationService
import services.plan.pointtopoint.{Path, PointToPointRouteFinderService}
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@Singleton
class TimetableFindRouteResultController @Inject()(
                                                    cc: ControllerComponents,
                                                    authenticatedUserAction: AuthorizedWebAction,
                                                    pathService: PointToPointRouteFinderService,
                                                    locationService: LocationService,
                                                    locationsService: LocationVisitService,
                                                    timetableService: TrainTimetableService,
                                                    routesService: RouteVisitService,
                                                    jwtService: JWTService

                                                  ) extends AbstractController(cc) {

  def redirect() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val data = request.request.body.asFormUrlEncoded

    val date: Option[String] = FindRouteResultHelper.extractString(data, "date")
    val trainUid: Option[String] = FindRouteResultHelper.extractString(data, "trainUid")

    play.api.mvc.Results.Redirect(
      controllers.plan.route.find.result.timetable.routes.TimetableFindRouteResultController.timetable(trainUid.getOrElse(""), date.getOrElse(""))
    )
  }

  def timetable(trainUid: String, date: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {

      val visitMode: String = "visitAllRoutesAndPublicStops"
      val fromLocationIndex: Int = 0
      val toLocationIndex: Int = 0
      val includeNonPublicStops: Boolean = false
      val overrideDateAndTimeOfVisit: Boolean = false
      val overriddenStartDate: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)
      val overrideVisitDetails: Boolean = false
      val overriddenVisitName: String = ""
      val overriddenTrainUid: String = ""
      val overriddenTrainHeadcode: String = ""
      val locationsToVisit: List[Location] = List.empty
      val routesToVisit: List[Route] = List.empty

      val token = jwtService.createToken(request.user, new Date())


      val followFixedLinks: Boolean = true
      val followFreightLinks: Boolean = true
      val followUnknownLinks: Boolean = true

      var messages = List.empty[String]

      if(trainUid.isEmpty || trainUid.isBlank) messages = "Please specify a train UID" :: messages

      val (year, month, day) = {
        try {
          val d = LocalDate.parse(date)
          (d.getYear, d.getMonthValue, d.getDayOfMonth)
        }
        catch {
          case e: Throwable =>
            messages =  s"Please specify a valid date in the format year-month-day, or leave blank for today - $e" :: messages
            (LocalDate.now.getYear, LocalDate.now.getMonthValue, LocalDate.now.getDayOfMonth)
        }
      }

      println(s"Building results for train ${trainUid} on date ${date} - $year-$month-$day Options: follow freight: $followFreightLinks, follow fixed: $followFixedLinks, follow unknown: $followUnknownLinks")

      val timetableF = timetableService
        .getTrain(trainUid, year.toString, month.toString, day.toString)

      val timetableOpt = Await.result(timetableF, Duration(30, TimeUnit.SECONDS))

      if(timetableOpt.isEmpty) {
        messages = s"Timetable for train ${trainUid} on date ${date} could not be found" :: messages
        play.api.mvc.Results.NotFound(views.html.plan.route.find.timetable.index(
          request.user,
          token,
          trainUid,
          date,
          controllers.plan.route.find.result.timetable.routes.TimetableFindRouteResultController.redirect(),
          messages
        ))
      }
      else {


        val timetable = timetableOpt.get

        val path: Path = pathService.findRouteForWaypoints(timetable.locations.map(_.tiploc), followFixedLinks, followFreightLinks, followUnknownLinks)

        val locationsList = path.locations

        val routeList = path.routes
        val waypoints = path.locations.map(l => {
          val isPublicStop = timetable.locations.find(_.tiploc.equals(l.id)).exists(t => t.publicArrival.isDefined || t.publicDeparture.isDefined)
          Waypoint(l.id, l.name, isPublicStop)
        })

        val distance = path.routes.map(_.distance).sum
        val time = {
          val startDate: LocalDate = LocalDate.parse(date)
          val startTime: LocalTime = timetable.locations.headOption.flatMap(_.departure).getOrElse(LocalTime.now())
          val endTime: LocalTime = timetable.locations.lastOption.flatMap(_.arrival).getOrElse(LocalTime.now())

          val endDate = if(endTime.isBefore(startTime)) startDate.plusDays(1) else startDate

          val startDateTime: LocalDateTime = LocalDateTime.of(startDate, startTime)
          val endDateTime: LocalDateTime = LocalDateTime.of(endDate, endTime)

          val diff = java.time.Duration.between(startDateTime, endDateTime)
          diff.getSeconds
        }

        val overriddenStartTime: LocalTime = timetable
          .locations
          .headOption
          .flatMap(_.departure)
          .getOrElse(LocalTime.now())

        val overriddenStartTimeStr = overriddenStartTime.format(DateTimeFormatter.ISO_TIME)



        play.api.mvc.Results.Ok(views.html.plan.route.find.timetable.result.index(
        request.user,
        token,
        ResultViewModel(
        locationsList,
        routeList,
        waypoints,
        path.followFreightLinks,
        path.followFixedLinks,
        path.followUnknownLinks,
        distance,
        mkTime(time, " (according to schedule)"),
        false,
        false,
        true,
        Some(s"${timetable.basicSchedule.trainUid} (${timetable.basicSchedule.trainIdentity})"),
        Some(LocalDate.now),
        timetable.locations.headOption.flatMap(l => locationService.findFirstLocationByTiploc(l.tiploc)),
        timetable.locations.lastOption.flatMap(l => locationService.findFirstLocationByTiploc(l.tiploc)),
        visitMode,
        fromLocationIndex,
        toLocationIndex,
        includeNonPublicStops,
        overrideDateAndTimeOfVisit,
        overriddenStartDate,
        overriddenStartTimeStr,
        overrideVisitDetails,
        overriddenVisitName,
        if(overriddenTrainUid.isBlank) timetable.basicSchedule.trainUid else overriddenTrainUid,
        if(overriddenTrainHeadcode.isBlank) timetable.basicSchedule.trainIdentity else overriddenTrainHeadcode,
        locationsToVisit,
        routesToVisit,
        controllers.plan.route.find.result.timetable.visit.routes.FindTimetableRouteResultVisitController.visit(trainUid, date),
        controllers.plan.route.find.timetable.routes.TimetableRouteController.index(trainUid, date)
        ),
        messages))
      }

    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

}
