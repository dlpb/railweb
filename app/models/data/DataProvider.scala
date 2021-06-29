package models.data

import models.auth.User
import models.location.Location
import models.route.Route
import services.location.LocationService
import services.route.RouteService

trait DataProvider[TypeOfThingVisited, MemoryModelVisitType <: Visit[TypeOfThingVisited]] {

  def getVisits(user: User): List[DataModelVisit]
  def getVisitsAsJson(user: User): String
  def saveVisit(id: DataModelVisit, user: User): Unit
  def saveVisits(visits: List[DataModelVisit], user: User): Unit
  def saveVisits(json: String, user: User): Unit
  def removeLastVisit(id: DataModelVisit, user: User): Unit
  def removeAllVisits(id: DataModelVisit, user: User): Unit

  def getIdForData(data: TypeOfThingVisited): String
  def getDataForId(id: String): TypeOfThingVisited

  def mapMemoryModelToDataModel(visit: MemoryModelVisitType): DataModelVisit
  def mapDataModelToMemoryModel(visit: DataModelVisit): MemoryModelVisitType

}

trait LocationDataProvider extends DataProvider[Location, LocationVisit] {

  def locationService: LocationService

  override def getDataForId(id: String): Location = locationService.findFirstLocationByIdOrCrs(id).get

  override def getIdForData(data: Location): String = data.id

  override def mapMemoryModelToDataModel(visit: LocationVisit): DataModelVisit = DataModelVisit(getIdForData(visit.visited), visit.created, visit.eventOccurredAt, visit.description, visit.trainUid)

  override def mapDataModelToMemoryModel(visit: DataModelVisit): LocationVisit = LocationVisit(getDataForId(visit.visited), visit.created, visit.eventOccurredAt, visit.description, visit.trainUid)
}
trait RouteDataProvider extends DataProvider[Route, RouteVisit] {

  def routeService: RouteService

  override def getIdForData(data: Route): String = s"from:${data.from.id}-to:${data.to.id}"

  override def getDataForId(id: String): Route = {
    val parts = id.split("-")
    val from = parts(0).split(":")(1)
    val to = parts(1).split(":")(1)
    routeService.findRoute(from, to).getOrElse(throw new IllegalArgumentException(s"Loading Route - could not find route from $from to $to"))
  }

  override def mapMemoryModelToDataModel(visit: RouteVisit): DataModelVisit = DataModelVisit(getIdForData(visit.visited), visit.created, visit.eventOccurredAt, visit.description, visit.trainUid)

  override def mapDataModelToMemoryModel(visit: DataModelVisit): RouteVisit = RouteVisit(getDataForId(visit.visited), visit.created, visit.eventOccurredAt, visit.description, visit.trainUid)
}

trait EventDataProvider {
  def getEvents(user: User): List[Event]
  def saveEvent(event: Event, user: User): Unit
  def saveEvents(events: List[Event], user: User): Unit
  def removeLastEvent(events: List[Event], user: User): Unit
  def removeAllEvents(events: List[Event], user: User): Unit
  def getEventsAsJson(user: User): String
  def saveEventsAsJson(events: String, user: User) : Unit
}