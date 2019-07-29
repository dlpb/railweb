package models.timetable

case class SimpleTimetable(
  basicSchedule: BasicSchedule,
  location: Location,
  origin: Location,
  destination: Location)
