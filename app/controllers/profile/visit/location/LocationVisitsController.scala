package controllers.profile.visit.location

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.data.{Event, LocationVisit}
import models.location.{Location}
import play.api.mvc.{AbstractController, AnyContent, Call, ControllerComponents}
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


  def index(sortField: String, sortOrder: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val visits: List[LocationVisit] =
      locationVisitsService
        .getVisitsForUser(request.user)
        .distinctBy(_.visited)

    val locationVisitEvents: Map[String, List[Event]] = visits.map(visit => {
      val events = locationVisitsService.getEventsLocationWasVisited(visit.visited, request.user).distinctBy(_.id)
      visit.visited.id -> events
    }).toMap

    val allVisitedLocations = locationVisitsService.getVisitsForUser(request.user)


    val locationVisitCount: Map[String, Int] = allVisitedLocations
      .groupBy(_.visited)
      .iterator
      .map(l => l._1.id -> l._2.size)
      .toMap

    val locationVisitIndex: Map[String, Int] = allVisitedLocations  //get all visits for all locations
      .groupBy(_.visited)                                           //group by location
      .iterator
      .map(l => {                                                   //map over the group of location to visit
        val location = l._1
        val visits = l._2
        val sortedVisits = visits.sortBy(_.eventOccurredAt)         //sort visits by date
        val earliestVisit = sortedVisits.head                       //take the first
        location -> earliestVisit.eventOccurredAt                   //make a new map of location to first visit
      })
      .toList
      .sortBy(_._2)                                                 //sort new map by first visits
      .map(_._1.id)                                                 //drop the visit date as it's now not needed
      .zipWithIndex                                                 //zip with index to get the order
      .map(l => l._1 -> (l._2 + 1))                                 //add one to make it human readable
      .toMap


    val sortedVisits = (sortField, sortOrder) match {
      case ("date", "asc") => visits.sortBy(_.eventOccurredAt)
      case ("date", "desc") => visits.sortBy(_.eventOccurredAt).reverse
      case ("name", "asc") => visits.sortBy(_.visited.name)
      case ("name", "desc") => visits.sortBy(_.visited.name).reverse
      case ("id", "asc") => visits.sortBy(_.visited.id)
      case ("id", "desc") => visits.sortBy(_.visited.id).reverse
      case ("count", "asc") => {
        visits.map(v => {
            val count = locationVisitCount(v.visited.id)
            v -> count
          })
          .sortBy(_._2)
          .map(_._1)
      }

      case ("count", "desc") => {
        visits.map(v => {
            val count = locationVisitCount(v.visited.id)
            v -> count
          })
          .sortBy(_._2)
          .reverse
          .map(_._1)
      }
      case _ => visits.sortBy(_.visited.name)
    }
    val locations: List[Location] = visits
      .sortBy(_.eventOccurredAt)
      .map(v => v.visited)

    val call: Call = routes.LocationVisitsController.index(sortField, sortOrder)

    Ok(views.html.visits.location.index(request.user, sortedVisits, locationVisitEvents, locations, locationVisitCount, locationVisitIndex, call, sortField, sortOrder))

  }
}

