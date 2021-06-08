package models.timetable.model.train

import java.time.LocalTime

case class IndividualTimetableLocation(
  tiploc: String,
  platform: String,
  line: String,
  engineeringAllowance: Int,
  engineeringAllowanceHalfMinute: Boolean,
  pathingAllowance: Int,
  pathingAllowanceHalfMinute: Boolean,
  performanceAllowance: Int,
  performanceAllowanceHalfMinute: Boolean,
  arrival: Option[LocalTime],
  arrivalHalfMinute: Option[Boolean],
  departure: Option[LocalTime],
  departureHalfMinute: Option[Boolean],
  pass: Option[LocalTime],
  passHalfMinute: Option[Boolean],
  path: String,
  publicArrival: Option[LocalTime],
  publicDeparture: Option[LocalTime])

