package models.location

import com.typesafe.config.Config
import javax.inject.Inject
import models.auth.User
import models.data.LocationDataProvider
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source

class LocationsService @Inject() ( config: Config,
                                   dataProvider: LocationDataProvider) {

  private val dataRoot = config.getString("data.static.root")
  private val locations = makeLocations(readLocationsFromFile)

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

  def readLocationsFromFile: String = {
    Source.fromFile(dataRoot + "/locations.json").mkString
  }

  def makeLocations(locations: String): Set[Location] = {
    implicit val formats = DefaultFormats
    parse(locations).extract[Set[Location]]
  }
}