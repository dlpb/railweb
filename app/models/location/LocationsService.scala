package models.location

import java.io.InputStream

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

  def defaultListLocations: List[ListLocation] = {

    def sortLocations(a: ListLocation, b: ListLocation): Boolean = {
      if(a.operator.equals(b.operator)) {
        if(a.srs.equals(b.srs))
          a.name < b.name
        else a.srs < b.srs
      }
      else a.operator < b.operator
    }

    val listItems = locations map { l => ListLocation(l) }
    listItems.toList.sortWith(sortLocations)

  }

  def getVisitedLocations(user: User): List[String] = {
    dataProvider.getVisits(user).map {
      data =>
        data.keySet.toList
    } .getOrElse(List())
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
    println(s"Location Tracing === saving location $location")
    dataProvider.saveVisit(location, user)
  }

  def deleteLastVisit(location: Location, user: User): Unit = {
    dataProvider.removeLastVisit(location, user)
  }

  def deleteAllVisits(location: Location, user: User): Unit = {
    dataProvider.removeAllVisits(location, user)
  }

  def readLocationsFromFile: String = {
    val path = "/data/static/locations.json"
    val data: InputStream = getClass().getResourceAsStream(path)
    Source.fromInputStream(data).mkString  }

  def makeLocations(locations: String): Set[Location] = {
    implicit val formats = DefaultFormats
    parse(locations).extract[Set[Location]]
  }
}