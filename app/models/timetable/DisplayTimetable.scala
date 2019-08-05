package models.timetable

import models.location.LocationsService
import models.location.{Location => DetailedLocation}

class DisplayTimetable(locationsService: LocationsService) {
  def apply(simpleTimetable: SimpleTimetable) = {
    DisplaySimpleTimetable(simpleTimetable,
      locationsService.findLocation(simpleTimetable.origin.tiploc),
      locationsService.findLocation(simpleTimetable.location.tiploc),
      locationsService.findLocation(simpleTimetable.destination.tiploc))
  }
}

case class DisplaySimpleTimetable(
                                   timetable: SimpleTimetable,
                                   origin: Option[DetailedLocation],
                                   location: Option[DetailedLocation],
                                   destination: Option[DetailedLocation])

