package models.location

case class Location(
                   id: String,
                   name: String,
                   operator: String,
                   `type`: String,
                   lat: Double,
                   lon: Double,
                   orrStation: Boolean,
                   crs: Set[String],
                   tiploc: Set[String],
                   srs: String,
                   orrId: Option[String] = None
                   ) {
  def isOrrStation = orrStation && orrId.nonEmpty
  def getLocationType = `type`
}

case class LocationDetail (
  id: String,
  crp: String,
  route: String,
  changeTime: String,
  interchangeType: String,
  county: String,
  district: String,
  postcode: String
                          )
