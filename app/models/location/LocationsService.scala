package models.location

import org.json4s.jackson.JsonMethods._
import org.json4s._

import scala.io.Source

class LocationsService {

  private val locations = LocationsService.makeLocations(LocationsService.readLocationsFromFile)

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

  def readLocationsFromFile: String = {
    Source.fromFile(System.getProperty("user.dir") + "/resources/data/static/locations.json").mkString
  }

  def makeLocations(locations: String): Set[Location] = {
    implicit val formats = DefaultFormats
    parse(locations).extract[Set[Location]]
  }
}