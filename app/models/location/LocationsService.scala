package models.location

import models.auth.User
import models.data.DataProvider
import org.json4s.jackson.JsonMethods._
import org.json4s._

import scala.io.Source

class LocationsService(dataProvider: DataProvider[Location]) {

  private val locations = LocationsService.makeLocations(LocationsService.readLocationsFromFile)

  def getLocation(id: String): Option[Location] =
    locations.find(_.id.equals(id))


  def mapLocations: Set[MapLocation] = {
    locations map { l => MapLocation(l) }
  }

  def defaultListLocations: Set[ListLocation] = {
    locations map { l => ListLocation(l) }
  }

  def getVisitsForLocation(location: Location, user: User): List[String] = {
    dataProvider.getVisits(user) flatMap {
      _.get(dataProvider.idToString(location))
    } match {
      case Some(list) => list
      case None => List()
    }
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

object LocationsService {

  def readLocationsFromFile: String = {
    Source.fromFile(System.getProperty("user.dir") + "/resources/data/static/locations.json").mkString
  }

  def makeLocations(locations: String): Set[Location] = {
    implicit val formats = DefaultFormats
    parse(locations).extract[Set[Location]]
  }
}