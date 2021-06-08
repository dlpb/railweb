package controllers.plan.route.find.pointtopoint

import java.net.URLDecoder
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import services.plan.pointtopoint.PointToPointRouteFinderService
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

@Singleton
class PointToPointRouteController @Inject()(
                                             cc: ControllerComponents,
                                             authenticatedUserAction: AuthorizedWebAction,
                                             pathService: PointToPointRouteFinderService,
                                             locationsService: LocationVisitService,
                                             routesService: RouteVisitService,
                                             jwtService: JWTService

                              ) extends AbstractController(cc) {

  def index(waypoints: String, followFixedLinks: Boolean, followFreightLinks: Boolean, followUnknownLinks: Boolean) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {


      val token = jwtService.createToken(request.user, new Date())

      val call = controllers.plan.route.find.result.pointtopoint.routes.PointToPointFindRouteResultController.pointtopoint()

      val messages = List()


      Ok(views.html.plan.route.find.pointtopoint.index(
        request.user,
        token,
        waypoints,
        followFreightLinks,
        followFixedLinks,
        followUnknownLinks,
        call,
        messages))


    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}