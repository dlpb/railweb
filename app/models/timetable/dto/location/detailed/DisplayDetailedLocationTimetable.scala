package models.timetable.dto.location.detailed

import models.location.LocationsService
import models.plan.timetable.trains.TrainTimetableServiceUrlHelper
import models.timetable.model.location.TimetableForLocation

object DisplayDetailedLocationTimetable {

  def apply(locationsService: LocationsService, simpleTimetable: TimetableForLocation, year: Int, month: Int, day: Int): DisplayDetailedLocationTimetable = {

    val public = simpleTimetable.publicTrain

    val arrival: String =
      if (simpleTimetable.pass.isDefined) "pass"
      else if (!public && simpleTimetable.arr.isDefined) simpleTimetable.arr.getOrElse("")
      else if (simpleTimetable.pubArr.isDefined) simpleTimetable.pubArr.getOrElse("")
      else ""

    val departure =
      if (simpleTimetable.pass.isDefined) simpleTimetable.pass.getOrElse("")
      else if (!public && simpleTimetable.dep.isDefined) simpleTimetable.dep.getOrElse("")
      else if (simpleTimetable.pubDep.isDefined) simpleTimetable.pubDep.getOrElse("")
      else ""

    val platform =
      if (simpleTimetable.pass.isDefined) ""
      else simpleTimetable.platform.getOrElse("")

    val toc = simpleTimetable.toc.getOrElse("Unknown TOC")

    DisplayDetailedLocationTimetable(
      public,
      simpleTimetable.pass.isDefined,
      simpleTimetable.uid,
      arrival,
      departure,
      simpleTimetable.origin.flatMap(o => locationsService.findLocationByTiploc(o).map(_.name)).getOrElse(simpleTimetable.origin.getOrElse("")),
      simpleTimetable.destination.flatMap(d => locationsService.findLocationByTiploc(d).map(_.name)).getOrElse(simpleTimetable.destination.getOrElse("")),
      platform,
      toc,
      simpleTimetable.stpIndicator.toString,
      TrainTimetableServiceUrlHelper.createUrlForDisplayingDetailedTrainTimetable(simpleTimetable.uid, year, month, day),
      "",
      "",
      ""
    )
  }
}

case class DisplayDetailedLocationTimetable(
                                    isPublic: Boolean,
                                    isPass: Boolean,
                                    uid: String,
                                    arrival: String,
                                    departure: String,
                                    origin: String,
                                    destination: String,
                                    platform: String,
                                    toc: String,
                                    stpIndicator: String,
                                    trainUrl: String,
                                    arrivalLabel: String,
                                    departureLabel: String,
                                    platformLabel: String
                                  )