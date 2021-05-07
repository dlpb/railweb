package controllers.profile.visit.event

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data
import models.visits.Event
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.visit.event.EventService
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

@Singleton
class EventVisitsController @Inject()(
                                       userDao: UserDao,
                                       jwtService: JWTService,
                                       cc: ControllerComponents,
                                       locationsService: LocationVisitService,
                                       routesService: RouteVisitService,
                                       eventService: EventService,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       authorizedAction: AuthorizedAction
                                     ) extends AbstractController(cc) {


  def index = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>

    val events: List[data.Event] = eventService.getEventsForUser(request.user)

    val eventsAndVisits = events.map(event => {
      val locationVisits = locationsService.getLocationsVisitedForEvent(event, request.user).size
      val routeVisits = routesService.getRoutesVisitedForEvent(event, request.user).size
      Event(event, routeVisits, locationVisits)
    })
      .sortBy(_.event.startedAt)

    Ok(views.html.visits.event.index(request.user, eventsAndVisits))

  }
}

