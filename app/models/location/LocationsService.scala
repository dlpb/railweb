package models.location

import org.json4s.jackson.JsonMethods._
import org.json4s._

class LocationsService(locations: Set[Location]) {
  def getLocation(id: String): Option[Location] =
    locations.find(_.id.equals(id))


  def mapLocations: Set[MapLocation] = {
    locations map {l => MapLocation(l)}
  }

  def defaultListLocations: Set[ListLocation] = {
    locations map {l => ListLocation(l)}
  }

}

object LocationsService {
  def makeLocationsService(locations: String): LocationsService = {
    implicit val formats = DefaultFormats
    new LocationsService(parse(locations).extract[Set[Location]]
    )
  }
}