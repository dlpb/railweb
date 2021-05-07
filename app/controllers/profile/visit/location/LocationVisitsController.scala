package controllers.profile.visit.location

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.{Event, LocationVisit, Visit}
import models.location.{Location, MapLocation}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.location.LocationService
import services.visit.event.EventService
import services.visit.location.LocationVisitService



@Singleton
class LocationVisitsController @Inject()(
                                          userDao: UserDao,
                                          jwtService: JWTService,
                                          cc: ControllerComponents,
                                          locationsService: LocationService,
                                          locationVisitsService: LocationVisitService,
                                          authenticatedUserAction: AuthorizedWebAction,
                                          eventService: EventService,
                                          authorizedAction: AuthorizedAction
                                        ) extends AbstractController(cc) {


  def index = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val visits: List[LocationVisit] =
      locationVisitsService
        .getVisitsForUser(request.user)
        .distinctBy(_.visited)

    val locationVisitEvents: Map[String, List[Event]] = visits.map(visit => {
      val events = locationVisitsService.getEventsLocationWasVisited(visit.visited, request.user).distinctBy(_.id)
      visit.visited.id -> events
    }).toMap

    val mapLocations: List[MapLocation] = visits
      .map(v => v.visited)
      .map(MapLocation(_))

    Ok(views.html.visits.location.index(request.user, visits, locationVisitEvents, mapLocations))

  }
}

