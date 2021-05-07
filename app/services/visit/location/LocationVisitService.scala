package services.visit.location

import java.time.LocalDateTime

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.{DataModelVisit, Event, LocationDataProvider, LocationVisit, RouteVisit, Visit}
import models.location.Location
import services.location.LocationService
import services.visit.event.EventService
  case class PathElementLocation(location: Location, adjacentPathElements: List[PathElementLocation]){
    override def toString: String = {
      location.id + "=>" + adjacentPathElements
    }
  }

@Singleton
class LocationVisitService @Inject() (config: Config,
                                      locationService: LocationService,
                                      eventService: EventService,
                                  dataProvider: LocationDataProvider) {

  def getVisitsForUser(user: User): List[LocationVisit] = {
    val visits: List[DataModelVisit] = dataProvider.getVisits(user)
    visits.map(dataProvider.mapDataModelToMemoryModel)
  }

  def saveVisits(visits: List[LocationVisit], user: User) = {
    dataProvider.saveVisits(visits.map(dataProvider.mapMemoryModelToDataModel), user)
  }

  def saveVisitsAsJson(json: String, user: User) = dataProvider.saveVisits(json, user)

  def getVisitsAsJson(user: User) = {
    dataProvider.getVisitsAsJson(user)
  }

  def getVisitedLocations(user: User): List[Location] = {
    getVisitsForUser(user).map {
      data =>
        data.visited
    }
  }

  def getVisitedLocationsByCrs(user: User): List[String] = {
    val orrLocs = locationService.locations.filter(_.orrId.isDefined).groupBy(_.orrId)

    getVisitsForUser(user).flatMap({
      visit => visit.visited.orrId.toList
    })
  }

  def getVisitsForLocation(location: Location, user: User): List[Visit[Location]] = {
    getVisitsForUser(user).filter(_.visited == location)
  }

  def isVisitFirstVisitForLocation(visit: Visit[Location], user: User, locationId: String): Boolean = {
    val visits = getVisitsForUser(user)

    val visitsForLocation = visits.filter(_.visited.id.equals(locationId))

    visitsForLocation
      .sortBy(_.eventOccurredAt)
      .headOption
      .exists(_.visited.id.equals(locationId))
  }

  val locationTiplocToCrs = locationService.locations
    .filterNot(_.crs.isEmpty)
    .map(l => l.id -> l.crs.headOption)
    .toMap


  def getStationVisitNumber(user: User, locationId: String): Option[Int] = {
    //    findLocationByTiploc(locationId).map(_.isOrrStation) map( _ => {
    //      val visits = getVisitsForUser(user)
    //        .getOrElse(Map.empty)
    //
    //      val stationVisits: List[(String, String)] = visits
    //        .flatMap(visitsForLocation => visitsForLocation._2.map(v => (visitsForLocation._1, v)))
    //        .toList
    //
    //      val sortedStationVisits: List[(String, String)] = stationVisits
    //        .sortBy(_._2)
    //        .map(v => {
    //          locationTiplocToCrs.get(v._1).flatten -> v._2
    //        })
    //        .filterNot(_._1.isEmpty)
    //        .map(v => v._1.head -> v._2)
    //        .distinctBy(_._1)
    //
    //
    //      val indexOfStation =
    //        locationTiplocToCrs.get(locationId).flatten.map(crs => {
    //          sortedStationVisits
    //            .indexWhere(v => v._1.equals(crs)) + 1
    //        }).getOrElse(0)
    //
    //     indexOfStation
    //    })
    None
  }

  def visitLocation(location: Location, user: User): Unit = {
    val visit = dataProvider.mapMemoryModelToDataModel(LocationVisit(location, LocalDateTime.now(), LocalDateTime.now(), "MANUAL_VISIT"))
    eventService.ensureActiveEvent(user)
    dataProvider.saveVisit(visit, user)
  }

  def deleteLastVisit(location: Location, user: User): Unit = {
    dataProvider.removeLastVisit(dataProvider.mapMemoryModelToDataModel(LocationVisit(location, LocalDateTime.MIN, LocalDateTime.MIN, "DELETE_LAST_VISIT")), user)
  }

  def deleteAllVisits(location: Location, user: User): Unit = {
    dataProvider.removeAllVisits(dataProvider.mapMemoryModelToDataModel(LocationVisit(location, LocalDateTime.MIN, LocalDateTime.MIN, "DELETE_ALL_VISIT")), user)
  }

  def getLocationsVisitedForEvent(event: Event, user: User): List[Location] = {
    val visits = getVisitsForUser(user)

    eventService.ensureAllVisitsHaveAnEvent(visits, user)

    visits
      .filter(v => v.eventOccurredAt.isBefore(event.endedAt) || v.eventOccurredAt.isEqual(event.endedAt))
      .filter(v => v.eventOccurredAt.isAfter(event.startedAt) || v.eventOccurredAt.isEqual(event.startedAt))
      .sortBy(_.eventOccurredAt)
      .map(_.visited)
  }

  def getEventsLocationWasVisited(location: Location, user: User): List[Event] = {

    val events = eventService.getEventsForUser(user)
    val locationVisits = getVisitsForLocation(location, user)

    eventService.ensureAllVisitsHaveAnEvent(locationVisits, user)

      val eventsForLocation: List[Event] = locationVisits
        .filter(_.visited.equals(location))
        .flatMap(v => {
          events
            .filter(e => v.eventOccurredAt.isBefore(e.endedAt) || v.eventOccurredAt.isEqual(e.endedAt))
            .filter(e => v.eventOccurredAt.isAfter(e.startedAt) || v.eventOccurredAt.isEqual(e.startedAt))
        })
        .distinctBy(_.id)
      eventsForLocation
  }

}