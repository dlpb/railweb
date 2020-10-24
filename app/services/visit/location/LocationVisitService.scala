package services.visit.location

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.LocationDataProvider
import models.location.Location
import services.location.LocationService
  case class PathElementLocation(location: Location, adjacentPathElements: List[PathElementLocation]){
    override def toString: String = {
      location.id + "=>" + adjacentPathElements
    }
  }

@Singleton
class LocationVisitService @Inject() (config: Config,
                                      locationService: LocationService,
                                  dataProvider: LocationDataProvider) {

  def getVisitsForUser(user: User): Option[Map[String, List[String]]] = {
    dataProvider.getVisits(user)
  }

  def saveVisits(visits: Option[Map[String, List[String]]], user: User) = {
    dataProvider.saveVisits(visits, user)
  }

  def getVisitedLocations(user: User): List[String] = {
    dataProvider.getVisits(user).map {
      data =>
        data.keySet.toList
    } .getOrElse(List())
  }

  def getVisitedLocationsByCrs(user: User): List[String] = {
    val orrLocs = locationService.locations.filter(_.orrId.isDefined).groupBy(_.orrId)

    dataProvider.getVisits(user).map {
      data =>
        val map: List[String] = data.keySet.toList.flatMap {
          tiploc =>
            val visitedTiplocForCrs: Set[String] = {
              val crsForTiploc: Set[String] = locationService.locations.filter(_.id.equals(tiploc)).flatMap(_.orrId)
              val locationsForTiploc: Set[Location] = crsForTiploc.flatMap { crs =>
                orrLocs.get(Some(crs))
              }.flatten
              locationsForTiploc.flatMap {
                _.crs
              }
            }

            val result: Set[String] = if (visitedTiplocForCrs.nonEmpty) visitedTiplocForCrs else Set()
            result
        }
        map
    }.getOrElse(Set()).toList
  }


  def getVisitsForLocation(location: Location, user: User): List[String] = {
    dataProvider.getVisits(user) flatMap {
      _.get(dataProvider.idToString(location))
    } match {
      case Some(list) => list
      case None => List()
    }
  }

  def getLocationsVisitedForEvent(event: String, user: User): List[Location] = {
    val filteredEvents: Map[String, List[String]] = getVisitsForUser(user)
      .getOrElse(Map.empty)
      .filter(_._2.contains(event))

    val locations: List[Location] = filteredEvents.keySet.flatMap({
        eventKey =>
         val locationIds = filteredEvents(eventKey)
         val locations = locationIds.flatMap(locationService.findFirstLocationByTiploc)
        locations
      })
      .toList

    locations
  }

  def isVisitFirstVisitForLocation(event: String, user: User, locationId: String): Boolean = {
   val visits = getVisitsForUser(user)
     .getOrElse(Map.empty)

    val thisVisit: Map[String, List[String]] = visits
     .filter(_._2.contains(event))
    val thisLocation: Map[String, List[String]] = thisVisit
     .filter(_._1.equals(locationId))
    val visitsToThisLocation: Iterable[String] =
      thisLocation
        .values
        .flatten
        .toList
        .sorted
    val firstVisitAtThisLocation = visitsToThisLocation.headOption.getOrElse("")
    val isThisFirstVisit = firstVisitAtThisLocation.equals(event)

    isThisFirstVisit

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
    dataProvider.saveVisit(location, user)
  }

  def deleteLastVisit(location: Location, user: User): Unit = {
    dataProvider.removeLastVisit(location, user)
  }

  def deleteAllVisits(location: Location, user: User): Unit = {
    dataProvider.removeAllVisits(location, user)
  }


}