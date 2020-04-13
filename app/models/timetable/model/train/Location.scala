package models.timetable.model.train

case class Location(
  tiploc: String,
  platform: String,
  line: String,
  engineeringAllowance: Int,
  engineeringAllowanceHalfMinute: Boolean,
  pathingAllowance: Int,
  pathingAllowanceHalfMinute: Boolean,
  performanceAllowance: Int,
   performanceAllowanceHalfMinute: Boolean,
  arrival: Option[Int],
  arrivalHalfMinute: Option[Boolean],
  departure: Option[Int],
  departureHalfMinute: Option[Boolean],
  pass: Option[Int],
  passHalfMinute: Option[Boolean],
  path: String,
  publicArrival: Option[Int], publicDeparture: Option[Int])

