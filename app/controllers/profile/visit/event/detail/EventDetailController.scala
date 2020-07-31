package controllers.profile.visit.event.detail

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.location.{LocationsService, MapLocation}
import models.route.{MapRoute, Route, RoutesService}
import models.visits.Event
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

@Singleton
class EventDetailController @Inject()(
                                       userDao: UserDao,
                                       jwtService: JWTService,
                                       cc: ControllerComponents,
                                       locationsService: LocationsService,
                                       routesService: RoutesService,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       authorizedAction: AuthorizedAction
                                     ) extends AbstractController(cc) {

    def index(event: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val visitedRoutes: List[Route] = routesService.getRoutesVisitedForEvent(event, request.user)
    val visitedLocations = locationsService
      .getLocationsVisitedForEvent(event, request.user)
      .map({
        MapLocation(_)
      })
      .sortBy(_.name)

    val distance: Long = visitedRoutes
      .map({
        _.distance
      })
      .sum

    val visitedMapRoutes = visitedRoutes
      .map {
        MapRoute(_)
      }
      .sortBy(r => r.from.id + " - " + r.to.id)

    Ok(views.html.visits.byEventDetail(request.user, visitedMapRoutes, visitedLocations, event, distance))

  }

}

