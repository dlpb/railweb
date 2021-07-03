package services.location

import com.typesafe.config.Config
import javax.inject.Inject
import models.helpers.JsonFileReader
import models.location.{GroupedListLocation, ListLocation, Location, MapLocation}

class LocationService @Inject() (config: Config) {
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
    val locs = locationFileReader.readAndParse[Set[Location]](config.getString("data.locations.path"))
    System.gc()
    locs
  }

  val mapLocations: Set[MapLocation] = locations.map(MapLocation(_))

  val sortedListLocationsGroupedByTiploc: List[ListLocation] = {
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

  val sortedListLocationsGroupedByCrs: List[GroupedListLocation] = {
    def sortLocations(a: GroupedListLocation, b: GroupedListLocation): Boolean = {
      if(a.operator.equals(b.operator)) {
        if(a.srs.equals(b.srs))
          a.name < b.name
        else a.srs < b.srs
      }
      else a.operator < b.operator
    }

    val groupedLocations: Map[String, List[Location]] = locations.toList
      .filter(_.orrId.isDefined)
      .groupBy(_.orrId.get)

    val groupedListLocations: List[GroupedListLocation] = groupedLocations.keySet.map({
      locationKey =>
        val locationsInGroup = groupedLocations(locationKey)
        val name = locationsInGroup.map(_.name).sortBy(_.length).headOption.getOrElse("")
        val toc = locationsInGroup.map(_.operator).headOption.getOrElse("XX")
        val `type` = locationsInGroup.map(_.`type`).headOption.getOrElse("")
        val orrStation = locationsInGroup.forall(_.orrStation)
        val srs = locationsInGroup.flatMap(_.nrInfo.map(_.srs)).headOption.getOrElse("")
        val relatedLocations = locationsInGroup.map(ListLocation(_))
        val orrId = locationsInGroup.flatMap(_.orrId).headOption

        val groupedLocation = GroupedListLocation(locationKey, name, toc, `type`,  orrStation, orrId, srs, relatedLocations)
        groupedLocation
    }).toList

    val groupedLocationsSorted = groupedListLocations.sortWith(sortLocations)
    groupedLocationsSorted
  }


  def findSingleLocationBy(predicate: Location => Boolean): Option[Location] = locations.find(predicate)

  def findAllLocationsBy(predicate: Location => Boolean): Set[Location] = locations.filter(predicate)

}
