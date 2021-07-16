package services.location

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.helpers.JsonFileReader
import models.location.{Location, LocationDetail}
import models.timetable.model.location.TimetableForLocationTypes.Tiploc

@Singleton
class LocationService @Inject() (config: Config) {
  def getAllLocationDetails(tiploc: String) : Set[LocationDetail] = {
    val locationsListPath = config.getString("data.locations.path")
    val locationDetails = config.getString("data.locations.details")

    val locationsPath = s"$locationsListPath$locationDetails$tiploc.json"
    println(s"Reading detail from $locationsPath")
    val locs = locationFileReader.readAndParse[Set[LocationDetail]](locationsPath)
    System.gc()
    locs
  }

  def findAllLocationsByCrsIdOrName(search: String): Set[Location] = if(search.isBlank) Set.empty else findAllLocationsBy(
    loc => loc.id.equalsIgnoreCase(search) || loc.name.equalsIgnoreCase(search) || loc.crs.map(_.toLowerCase()).contains(search.toLowerCase()))


  def findFirstLocationByTiploc(tiploc: String): Option[Location] = if(tiploc.isBlank) None else findSingleLocationBy(_.id.equalsIgnoreCase(tiploc))

  def findFirstLocationByCrs(crs: String): Option[Location] = if(crs.isBlank) None else findSingleLocationBy(l => l.orrId.isDefined && l.orrId.get.equals(crs))

  def findAllLocationsByCrs(crs: String): Set[Location] = if(crs.isBlank) Set.empty else findAllLocationsBy(_.crs.contains(crs))

  def findFirstLocationByIdOrCrs(key: String): Option[Location] =
    findSingleLocationBy(l =>
      l.id.equals(key) ||
        l.orrId.isDefined && l.orrId.get.equals(key)
    )

  def findFirstLocationByNameTiplocCrsOrId(key: String): Option[Location] = {
    findSingleLocationBy(l =>
        l.name.toUpperCase.equals(key.toUpperCase) ||
        l.id.toUpperCase.equals(key.toUpperCase) ||
        l.tiploc.map(_.toUpperCase).contains(key.toUpperCase) ||
        l.crs.map(_.toUpperCase).contains(key.toUpperCase)
    )
  }

  def findLocationByNameTiplocCrsIdPrioritiseOrrStations(key: String): Option[Location] = {
    val matchingLocsIncludingNonOrrStations = findAllLocationsBy(l =>
            l.name.toUpperCase.equals(key.toUpperCase) ||
            l.id.toUpperCase.equals(key.toUpperCase) ||
            l.tiploc.map(_.toUpperCase).contains(key.toUpperCase) ||
            l.crs.map(_.toUpperCase).contains(key.toUpperCase)
      )

    val matchingOrrLocations = matchingLocsIncludingNonOrrStations.filter(_.isOrrStation)
    if(matchingOrrLocations.nonEmpty) matchingOrrLocations.headOption
    else matchingLocsIncludingNonOrrStations.headOption
  }

  def findAllLocationsMatchingCrs(location: Location): Set[Location] = {
    location.crs.flatMap({ crs =>
      findAllLocationsBy(l => l.crs.contains(crs) && l.isOrrStation)
    })
  }

  private val locationFileReader = new JsonFileReader

  val locations: Set[Location] = {
    val locationsListPath = config.getString("data.locations.path")
    val locationsFileName = config.getString("data.locations.fileName")

    val locationsPath = s"$locationsListPath$locationsFileName"
    println(s"loading locatiosn from $locationsPath")
    val locs = locationFileReader.readAndParse[Set[Location]](locationsPath)
    System.gc()
    println("done")
    locs
  }

  val sortedLocationsGroupedByTiploc: List[Location] = {
    def sortLocations(a: Location, b: Location): Boolean = {
      if(a.operator.equals(b.operator)) {
        if(a.srs.equals(b.srs))
          a.name < b.name
        else a.srs < b.srs
      }
      else a.operator < b.operator
    }

    val listItems = locations
    listItems.toList.sortWith(sortLocations)
  }

  val sortedListLocationsGroupedByCrs: List[(Location, List[Location])] = {
    def sortLocations(a: (Location, List[Location]), b: (Location, List[Location])): Boolean = {
      if(a._1.operator.equals(b._1.operator)) {
        if(a._1.srs.equals(b._1.srs))
          a._1.name < b._1.name
        else a._1.srs < b._1.srs
      }
      else a._1.operator < b._1.operator
    }

    val groupedLocations: Map[String, List[Location]] = locations.toList
      .filter(_.orrId.isDefined)
      .groupBy(_.orrId.get)

    val groupedListLocations = groupedLocations.keySet.map({
      locationKey =>

        val location = findAllLocationsByCrsIdOrName(locationKey).head

        val groupedLocation = (location, groupedLocations(locationKey))
        groupedLocation
    }).toList

    val groupedLocationsSorted = groupedListLocations.sortWith(sortLocations)
    groupedLocationsSorted
  }


  def findSingleLocationBy(predicate: Location => Boolean): Option[Location] = locations.find(predicate)

  def findAllLocationsBy(predicate: Location => Boolean): Set[Location] = locations.filter(predicate)

}
