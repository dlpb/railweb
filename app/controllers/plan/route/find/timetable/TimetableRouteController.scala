package controllers.plan.route.find.timetable

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.plan.pointtopoint.PointToPointRouteFinderService
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

@Singleton
class TimetableRouteController @Inject()(
                                             cc: ControllerComponents,
                                             authenticatedUserAction: AuthorizedWebAction,
                                             pathService: PointToPointRouteFinderService,
                                             locationsService: LocationVisitService,
                                             routesService: RouteVisitService,
                                             jwtService: JWTService

                              ) extends AbstractController(cc) {

  def index(trainUid: String, date: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {


      val token = jwtService.createToken(request.user, new Date())

      val call = controllers.plan.route.find.result.timetable.routes.TimetableFindRouteResultController.timetable()

      val messages = List()


      Ok(views.html.plan.route.find.timetable.index(
        request.user,
        token,
        trainUid,
        date,
        call,
        messages))


    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}