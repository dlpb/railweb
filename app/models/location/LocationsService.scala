package models.location

import java.io.InputStream

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.LocationDataProvider
import models.route.{RoutePoint, RoutesService}
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source
  case class PathElementLocation(location: Location, adjacentPathElements: List[PathElementLocation]){
    override def toString: String = {
      location.id + "=>" + adjacentPathElements
    }
  }

@Singleton
class LocationsService @Inject() ( config: Config,
                                   routesService: RoutesService,
                                   dataProvider: LocationDataProvider) {

  def findAdjacentLocations(startingPoint: Location,
                            orrStationMaxDepth: Int = 2,
                            nonOrrStationMaxDepth: Int = 2,
                            countNonOrrStations: Boolean = false,
                            path: List[PathElementLocation] = List.empty,
                            visitedLocIds: List[String] = List.empty
                           ): List[PathElementLocation] = {

    def findAdjacentLocations0(startingPoint: Location,
                               orrStationMaxDepth: Int,
                               nonOrrStationMaxDepth: Int,
                               countNonOrrStations: Boolean,
                               path: List[PathElementLocation],
                               visitedLocIds: List[String]
                              ): List[PathElementLocation] = {


      if (orrStationMaxDepth == 0 || nonOrrStationMaxDepth == 0) {
        path
      }
      else {
        val routesForLocation = routesService.findRoutesForLocation(startingPoint.id)
        val endPoints: List[RoutePoint] = routesForLocation
          .map(r => {
            if (r.from.id.equals(startingPoint.id)) r.to else r.from
          })
          .filterNot(endpoint => visitedLocIds.contains(endpoint.id))
          .toList


        val adjacentPathElements: List[PathElementLocation] = endPoints.map(endpoint => {
          val endpointLoc = findLocation(endpoint.id).get

          val isOrrStation = endpointLoc.isOrrStation
          val newOrrStationMaxDepth = if (isOrrStation) orrStationMaxDepth - 1 else orrStationMaxDepth
          val newNonOrrStationMaxDepth = if (isOrrStation) nonOrrStationMaxDepth else nonOrrStationMaxDepth - 1
          val newVisitedLocIds = visitedLocIds :+ endpointLoc.id
          val newPath = path.filterNot(p => visitedLocIds.contains(p.location.id))

          val adjacent = PathElementLocation(endpointLoc, findAdjacentLocations0(endpointLoc, newOrrStationMaxDepth, newNonOrrStationMaxDepth, countNonOrrStations, newPath, newVisitedLocIds))
          adjacent
        })

        val pathElements = adjacentPathElements

        pathElements

      }
    }
    val result = List(PathElementLocation(startingPoint, findAdjacentLocations0(startingPoint, orrStationMaxDepth, nonOrrStationMaxDepth, countNonOrrStations, List(PathElementLocation(startingPoint, List.empty)), List(startingPoint.id))))
    println(result)
    result
  }


  def getTimetableLocations: Set[String] = {
    implicit val formats = DefaultFormats
    val json = Source.fromURL("http://rail.dlpb.uk/data/timetables/locationkeys.json").getLines().mkString
    val locations = parse(json).extract[Map[String, String]]
    locations.values.toSet
  }


  private val dataRoot = config.getString("data.static.root")
  private val locations: Set[Location] = makeLocations(readLocationsFromFile)

  def getLocations = locations.toList

  def findLocation(key: String): Option[Location] = {
    locations.find(l =>
      l.id.toUpperCase.equals(key.toUpperCase) ||
        l.tiploc.map(_.toUpperCase).contains(key.toUpperCase) ||
        l.crs.map(_.toUpperCase).contains(key.toUpperCase) ||
        l.name.toUpperCase.equals(key.toUpperCase)
    )
  }

  def findPriortiseOrrStations(key: String): Option[Location] = {
    val matchingLocsIncludingNonOrrStations = locations
      .filter(l =>
        l.id.toUpperCase.equals(key.toUpperCase) ||
        l.tiploc.map(_.toUpperCase).contains(key.toUpperCase) ||
        l.crs.map(_.toUpperCase).contains(key.toUpperCase) ||
        l.name.toUpperCase.equals(key.toUpperCase)
    )

    val matchingOrrLocations = matchingLocsIncludingNonOrrStations.filter(_.isOrrStation)
    if(matchingOrrLocations.nonEmpty) matchingOrrLocations.headOption
    else matchingLocsIncludingNonOrrStations.headOption
  }

  def findAllLocationsMatchingCrs(key: String): Set[Location] = {
    val initialLocation = findLocation(key)
    if(initialLocation.isDefined){
      val location = initialLocation.get
      if(location.isOrrStation){
        location.crs.flatMap({ crs =>
          val locs = locations.filter(l => l.crs.contains(crs) && l.isOrrStation)
          locs
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