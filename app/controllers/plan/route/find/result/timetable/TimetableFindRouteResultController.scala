package controllers.plan.route.find.result.timetable

import java.net.URLDecoder
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime}
import java.util.Date
import java.util.concurrent.TimeUnit

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import controllers.plan.route.find.result.FindRouteResultHelper.mkTime
import controllers.plan.route.find.result.{FindRouteResultHelper, ResultViewModel, Waypoint}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.{Location, MapLocation}
import models.plan.timetable.trains.TrainTimetableService
import models.route.Route
import models.route.display.map.MapRoute
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
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
                                                    locationsService: LocationVisitService,
                                                    timetableService: TrainTimetableService,
                                                    routesService: RouteVisitService,
                                                    jwtService: JWTService

                                                  ) extends AbstractController(cc) {

  def timetable() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {

      val visitMode: String = "visitAllRoutesAndPublicStops"
      val fromLocationIndex: Int = 0
      val toLocationIndex: Int = 0
      val includeNonPublicStops: Boolean = false
      val overrideDateAndTimeOfVisit: Boolean = false
      val overriddenStartDate: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)
      val overriddenStartTime: String = LocalTime.now.format(DateTimeFormatter.ISO_TIME)
      val overrideVisitDetails: Boolean = false
      val overriddenVisitName: String = ""
      val overriddenTrainUid: String = ""
      val overriddenTrainHeadcode: String = ""
      val locationsToVisit: List[Location] = List.empty
      val routesToVisit: List[Route] = List.empty

      val token = jwtService.createToken(request.user, new Date())

      val data = request.request.body.asFormUrlEncoded

      val date: Option[String] = FindRouteResultHelper.extractString(data, "date")
      val trainUid: Option[String] = FindRouteResultHelper.extractString(data, "trainUid")
      val followFixedLinks: Boolean = true
      val followFreightLinks: Boolean = true
      val followUnknownLinks: Boolean = true

      var messages = List.empty[String]

      if(trainUid.isEmpty || trainUid.get.isBlank) messages = "Please specify a train UID" :: messages

      val (year, month, day) = {
        try {
          val d = date
            .map(LocalDate.parse(_))
            .getOrElse(LocalDate.now())
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
        .getTrain(trainUid.get, year.toString, month.toString, day.toString)

      val timetableOpt = Await.result(timetableF, Duration(30, TimeUnit.SECONDS))

      if(timetableOpt.isEmpty) messages = s"Timetable for train $trainUid on date $date could not be found" :: messages

      val timetable = timetableOpt.get

      val path: Path = pathService.findRouteForWaypoints(timetable.locations.map(_.tiploc), followFixedLinks, followFreightLinks, followUnknownLinks)

      val mapLocationList = path.locations.map(MapLocation(_))
      val locationsToRouteVia = path.locations.map(_.id)

      val mapRouteList = path.routes.map(r => MapRoute(r))
      val routeList = path.routes
      val waypoints = path.locations.map(l => Waypoint(l.id, l.name, l.isOrrStation))

      val distance = path.routes.map(_.distance).sum
      val time = path.routes.map(_.travelTimeInSeconds).map(_.getSeconds).sum



      play.api.mvc.Results.Ok(views.html.plan.route.find.timetable.result.index(
        request.user,
        token,
        ResultViewModel(
          mapLocationList,
          mapRouteList,
          routeList,
          waypoints,
          path.followFreightLinks,
          path.followFixedLinks,
          path.followUnknownLinks,
          distance,
          mkTime(time, " (assuming no stops)"),
          true,
          false,
          false,
          None,
          Some(LocalDate.now),
          None,
          None,
          visitMode,
          fromLocationIndex,
          toLocationIndex,
          includeNonPublicStops,
          overrideDateAndTimeOfVisit,
          overriddenStartDate,
          overriddenStartTime,
          overrideVisitDetails,
          overriddenVisitName,
          overriddenTrainUid,
          overriddenTrainHeadcode,
          locationsToVisit,
          routesToVisit,
          controllers.plan.route.find.result.visit.routes.FindRouteResultVisitController.visit(),
          controllers.plan.route.find.pointtopoint.routes.PointToPointRouteController.index(locationsToRouteVia.mkString("\n"), path.followFixedLinks, path.followFreightLinks, path.followUnknownLinks)
        ),
        messages))

    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

}
