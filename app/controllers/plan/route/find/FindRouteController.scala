package controllers.plan.route.find

import java.net.URLDecoder
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.MapLocation
import services.plan.pointtopoint.PointToPointRouteFinderService
import models.route.display.map.MapRoute
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

@Singleton
class FindRouteController @Inject()(
                                     cc: ControllerComponents,
                                     authenticatedUserAction: AuthorizedWebAction,
                                     pathService: PointToPointRouteFinderService,
                                     locationsService: LocationVisitService,
                                     routesService: RouteVisitService,
                                     jwtService: JWTService

                                   ) extends AbstractController(cc) {

  def index() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {

      val token = jwtService.createToken(request.user, new Date())


        Ok(views.html.plan.route.find.index(
          request.user,
          token))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}