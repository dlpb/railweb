package models.timetable.dto.location.simple

import models.plan.timetable.trains.TrainTimetableServiceUrlHelper
import models.timetable.dto.location.SortableDisplayTimetable
import models.timetable.model.location.TimetableForLocation
import services.location.LocationService

object DisplaySimpleLocationTimetable {

  def apply(locationsService: LocationService, simpleTimetable: TimetableForLocation, year: Int, month: Int, day: Int): DisplaySimpleLocationTimetable = {
    val arrival = simpleTimetable.pubArr.map(_.toString).getOrElse("")
    val departure = simpleTimetable.pubDep.map(_.toString).getOrElse("")
    val platform = simpleTimetable.platform.map(_.toString).getOrElse("")
    val toc = simpleTimetable.toc.getOrElse("")

    DisplaySimpleLocationTimetable(
      arrival,
      departure,
      simpleTimetable.origin.flatMap({ o => locationsService.findFirstLocationByTiploc(o).map(_.name)}).getOrElse(simpleTimetable.origin.getOrElse("")),
      simpleTimetable.destination.flatMap({ o => locationsService.findFirstLocationByTiploc(o).map(_.name)}).getOrElse(simpleTimetable.destination.getOrElse("")),
      platform,
      TrainTimetableServiceUrlHelper.createUrlForDisplayingTrainSimpleTimetable(simpleTimetable.uid, year, month, day),
      if(arrival != "") "Arr." else "",
      if(departure != "") "Dep." else "",
      if(platform != "") "Platform" else "",
      if(toc != "") toc else ""
    )
  }
}

case class DisplaySimpleLocationTimetable(
                                    arrival: String,
                                    departure: String,
                                    origin: String,
                                    destination: String,
                                    platform: String,
                                    trainUrl: String,
                                    arrivalLabel: String,
                                    departureLabel: String,
                                    platformLabel: String,
                                    toc: String
                                  ) extends SortableDisplayTimetable
