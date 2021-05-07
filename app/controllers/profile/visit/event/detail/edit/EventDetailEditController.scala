package controllers.profile.visit.event.detail.edit

import java.time.LocalDateTime
import java.util.Date

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.Event
import models.location.MapLocation
import models.route.Route
import models.route.display.map.MapRoute
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.location.LocationService
import services.visit.event.EventService
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

import scala.util.Success

@Singleton
class EventDetailEditController @Inject()(
                                       userDao: UserDao,
                                       jwtService: JWTService,
                                       cc: ControllerComponents,
                                       locationsService: LocationService,
                                       locationVisitService: LocationVisitService,
                                       routeVisitService: RouteVisitService,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       eventService: EventService,
                                       authorizedAction: AuthorizedAction
) extends AbstractController(cc) {



  def index(id: String) = authenticatedUserAction {
    implicit request: WebUserContext[AnyContent] =>
      val eventOption = eventService.getEventFromId(id, request.user)
      val call = controllers.profile.visit.event.detail.edit.routes.EventDetailEditController.post(id)
      val token = jwtService.createToken(request.user, new Date())
      eventOption
        .map({ event =>
          Ok(
            views.html.visits.event.detail.edit.index(
              request.user,
              call,
              token,
              event,
              List.empty
            )
          )
        })
        .getOrElse(
          NotFound(
            views.html.visits.event.detail.edit.index(
              request.user,
              call,
              token,
              Event(),
              List(s"No Event for ${id} found. Please go back and try again")
            )
          )
        )
  }

  def post(id: String) = authenticatedUserAction {
    implicit request: WebUserContext[AnyContent] =>

      val data = request.request.body.asFormUrlEncoded

      val receivedToken = data.get("Authorization")
      val call = controllers.profile.visit.event.detail.edit.routes.EventDetailEditController.post(id)
      val token = jwtService.createToken(request.user, new Date())

      jwtService.isValidToken(receivedToken.headOption.getOrElse("NO_TOKEN")) match {
        case Success(_) =>
          val eventOption = eventService.getEventFromId(id, request.user)
          val name = data.get("name").headOption
          val description = data.get("description").headOption
          val startTime = data.get("startTime").headOption
          val endTime = data.get("endTime").headOption

          (eventOption, name, description, startTime, endTime) match {
            case (Some(ev), Some(n), Some(d), Some(s), Some(e)) =>
              val events = eventService.getEventsForUser(request.user)
              val eventsMinusThisEvent = events.filterNot(_.id.equals(id))
              val newEvent = Event(
                id,
                n,
                d,
                ev.created,
                LocalDateTime.parse(s),
                LocalDateTime.parse(e)
              )
              val newEventList = newEvent :: eventsMinusThisEvent
              eventService.saveEvents(newEventList, request.user)
              Ok(
                views.html.visits.event.detail.edit.index(
                  request.user,
                  call,
                  token,
                  ev,
                  List.empty
                )
              )
            case (None, _, _, _, _) =>
              NotFound(
                views.html.visits.event.detail.edit.index(
                  request.user,
                  call,
                  token,
                  Event(),
                  List(s"Event for ID ${id} not found. Please go back and select another event")
                )
              )
            case (_, _, _, _, _) =>
              BadRequest(
                views.html.visits.event.detail.edit.index(
                  request.user,
                  call,
                  token,
                  Event(),
                  List(s"One or more fields were not supplied. Please go back and try again")
                )
              )
          }

        case _ => BadRequest(views.html.visits.event.detail.edit.index(
          request.user,
          call,
          token,
          Event(),
          List("Invalid User Token Found...")
        ))
      }
  }

}
