package models.timetable.dto.location.simple

import models.location.LocationsService
import models.plan.timetable.trains.{TrainTimetableService, TrainTimetableServiceUrlHelper}
import models.timetable.model.location.TimetableForLocation

object DisplaySimpleLocationTrain {

  def apply(locationsService: LocationsService, simpleTimetable: TimetableForLocation, year: Int, month: Int, day: Int): DisplaySimpleLocationTrain = {
    val arrival = simpleTimetable.pubArr.map(_.toString).getOrElse("")
    val departure = simpleTimetable.pubDep.map(_.toString).getOrElse("")
    val platform = simpleTimetable.platform.map(_.toString).getOrElse("")

    DisplaySimpleLocationTrain(
      arrival,
      departure,
      simpleTimetable.origin.flatMap({ o => locationsService.findLocation(o).map(_.name)}).getOrElse(simpleTimetable.origin.getOrElse("")),
      simpleTimetable.destination.flatMap({ o => locationsService.findLocation(o).map(_.name)}).getOrElse(simpleTimetable.destination.getOrElse("")),
      platform,
      TrainTimetableServiceUrlHelper.createUrlForDisplayingTrainSimpleTimetable(simpleTimetable.uid, year, month, day),
      if(arrival != "") "Arr." else "",
      if(departure != "") "Dep." else "",
      if(platform != "") "Platform" else ""
    )
  }

  def time(time: Int): String = {
    if(time < 10) s"000$time"
    else if (time < 100) s"00$time"
    else if (time < 1000) s"0$time"
    else s"$time"
  }
}

case class DisplaySimpleLocationTrain(
                                    arrival: String,
                                    departure: String,
                                    origin: String,
                                    destination: String,
                                    platform: String,
                                    trainUrl: String,
                                    arrivalLabel: String,
                                    departureLabel: String,
                                    platformLabel: String
                                  )
