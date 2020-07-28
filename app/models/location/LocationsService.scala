package models.location

import java.io.InputStream

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.LocationDataProvider
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source

@Singleton
class LocationsService @Inject() ( config: Config,
                                   dataProvider: LocationDataProvider) {


  def getTimetableLocations: Set[String] = {
    implicit val formats = DefaultFormats
    val json = Source.fromURL("http://rail.dlpb.uk/data/timetables/locationkeys.json").getLines().mkString
    val locations = parse(json).extract[Map[String, String]]
    locations.values.toSet
  }


  private val dataRoot = config.getString("data.static.root")
  private val locations: Set[Location] = makeLocations(readLocationsFromFile)

  def getLocations = locations.toList

  def findLocation(key: String): Option[Location] =
    locations.find(l =>
      l.id.toUpperCase.equals(key.toUpperCase) ||
      l.tiploc.map(_.toUpperCase).contains(key.toUpperCase)  ||
      l.crs.map(_.toUpperCase).contains(key.toUpperCase)  ||
      l.name.toUpperCase.equals(key.toUpperCase)

    )

  def findAllLocationsMatchingCrs(key: String): Set[Location] = {
    val initialLocation = findLocation(key)
    if(initialLocation.isDefined){
      val location = initialLocation.get
      if(location.isOrrStation){
        location.crs.flatMap({ crs =>
          locations.filter(l => l.crs.contains(crs) && l.isOrrStation)
        })
      }
      else {
        Set(location)
      }
    }
    else{
      Set.empty
    }
  }

  def getVisitsForUser(user: User): Option[Map[String, List[String]]] = {
    dataProvider.getVisits(user)
  }

  def saveVisits(visits: Option[Map[String, List[String]]], user: User) = {
    dataProvider.saveVisits(visits, user)
  }

  def getLocation(id: String): Option[Location] =
    locations.find(_.id.equals(id))

  def getLocationByIdOrOrrId(id: String): Option[Location] =
    locations.find(l =>
      l.id.equals(id) ||
      l.orrId.isDefined && l.orrId.get.equals(id)
    )


  def mapLocations: Set[MapLocation] = {
    locations map { l => MapLocation(l) }
  }

  def groupedByCrsListLocations: List[GroupedListLocation] = {
    def sortLocations(a: GroupedListLocation, b: GroupedListLocation): Boolean = {
      if(a.operator.equals(b.operator)) {
        if(a.srs.equals(b.srs))
          a.name < b.name
        else a.srs < b.srs
      }
      else a.operator < b.operator
    }

    val groupedLocations = locations.toList
      .filter(_.orrId.isDefined)
      .groupBy(_.orrId)
      .map{loc =>
        val name = loc._2.map(_.name).toList.sortBy(_.length).headOption.getOrElse("")
        val toc = loc._2.map(_.operator).headOption.getOrElse("XX")
        val `type` = loc._2.map(_.`type`).headOption.getOrElse("")
        val orrStation = loc._2.forall(_.orrStation)
        val srs = loc._2.flatMap(_.nrInfo.map(_.srs)).headOption.getOrElse("")
        val relatedLocations = loc._2.map(ListLocation(_))

        val locs = GroupedListLocation(loc._1.getOrElse(""), name, toc, `type`,  orrStation, srs, relatedLocations)
        locs
      }

    val groupedLocationsSorted = groupedLocations.toList.sortWith(sortLocations)
    groupedLocationsSorted
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

  def getVisitedLocationsByCrs(user: User): List[String] = {
    val orrLocs = locations.filter(_.orrId.isDefined).groupBy(_.orrId)

    dataProvider.getVisits(user).map {
      data =>
        val map: List[String] = data.keySet.toList.flatMap {
          tiploc =>
            val visitedTiplocForCrs: Set[String] = {
              val crsForTiploc: Set[String] = locations.filter(_.id.equals(tiploc)).flatMap(_.orrId)
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
    getVisitsForUser(user)
      .getOrElse(Map.empty)
      .filter(_._2.contains(event))
      .flatMap { location => getLocation(location._1)}
      .toList
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
    val path = "/data/static/locations.json"
    val data: InputStream = getClass().getResourceAsStream(path)
    Source.fromInputStream(data).mkString  }

  def makeLocations(locations: String): Set[Location] = {
    implicit val formats = DefaultFormats
    parse(locations).extract[Set[Location]]
  }
}