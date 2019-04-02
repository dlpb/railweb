package models.location

case class Location(
                   id: String,
                   name: String,
                   operator: String,
                   `type`: String,
                   location: SpatialLocation,
                   nrInfo: Option[NrInfo],
                   orrStation: Boolean,
                   crs: Set[String],
                   tiploc: Set[String]
                   )

case class SpatialLocation(
                          lat: Double,
                          lon: Double
                          )

case class NrInfo(
                 crp: String,
                 route: String,
                 srs: String,
                 changeTime: String,
                 interchangeType: String
                 )

case class MapLocation(id: String,
                       name: String,
                       operator: String,
                       `type`: String,
                       location: SpatialLocation,
                       orrStation: Boolean,
                       crs: Set[String],
                       tiploc: Set[String]
                      )
object MapLocation {
  def apply(location: Location): MapLocation = {
    new MapLocation(
      location.id,
      location.name,
      location.operator,
      location.`type`,
      location.location,
      location.orrStation,
      location.crs,
      location.tiploc
    )
  }
}

case class ListLocation(id: String,
                       name: String,
                       operator: String,
                       `type`: String,
                       orrStation: Boolean,
                        srs: String
                      )
object ListLocation {
  def apply(location: Location): ListLocation = {
    new ListLocation(
      location.id,
      location.name,
      location.operator,
      location.`type`,
      location.orrStation,
      location.nrInfo.map {_.srs}.getOrElse("")
    )
  }
}