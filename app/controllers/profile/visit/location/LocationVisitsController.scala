package controllers.profile.visit.location

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.postgres.RouteDataIdConverter
import models.location.{LocationsService, MapLocation}
import models.route.{MapRoute, Route, RoutesService}
import models.visits.Event
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

@Singleton
class LocationVisitsController @Inject()(
                                          userDao: UserDao,
                                          jwtService: JWTService,
                                          cc: ControllerComponents,
                                          locationsService: LocationsService,
                                          routesService: RoutesService,
                                          authenticatedUserAction: AuthorizedWebAction,
                                          authorizedAction: AuthorizedAction
                                        ) extends AbstractController(cc) {


  def index = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val locations: Map[String, List[String]] =
      locationsService
        .getVisitsForUser(request.user)
        .getOrElse(Map.empty[String, List[String]])
    val mapLocations: List[MapLocation] = locations
      .keySet
      .flatMap {
        locationsService.getLocation
      }
      .map {
        MapLocation(_)
      }
      .toList

    Ok(views.html.visits.byLocation(request.user, locations, mapLocations))

  }
}

