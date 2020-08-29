package models.timetable.dto.train.simple

import java.time.LocalDate

import models.location.LocationsService
import models.plan.timetable.TimetableDateTimeHelper
import models.plan.timetable.location.LocationTimetableServiceUrlHelper
import models.timetable.dto.TimetableHelper
import models.timetable.model.train.IndividualTimetable

object DisplaySimpleTrainTimetable {


  def apply(locationsService: LocationsService, tt: IndividualTimetable, year: Int, month: Int, day: Int): DisplaySimpleTrainTimetable = {
    val date = LocalDate.of(year, month, day)

    DisplaySimpleTrainTimetable(
      tt.basicScheduleExtraDetails.atocCode,
      tt.locations.headOption.flatMap(l => locationsService.findLocationByTiploc(l.tiploc).map(l => l.name)).getOrElse(""),
      tt.locations.lastOption.flatMap(l => locationsService.findLocationByTiploc(l.tiploc).map(l => l.name)).getOrElse(""),
      date,
      tt.locations
        .filter {
          l => l.pass.isEmpty && (l.publicDeparture.isDefined || l.publicArrival.isDefined)
        }
        .map {
          l =>
            val (hour, minute) = if (l.pass.isDefined) TimetableDateTimeHelper.hourMinute(l.pass.get)
            else if (l.publicArrival.isDefined) TimetableDateTimeHelper.hourMinute(l.publicArrival.get)
            else if (l.publicDeparture.isDefined) TimetableDateTimeHelper.hourMinute(l.publicDeparture.get)
            else (0, 0)

            val from = date.atTime(hour, minute).minusMinutes(15)
            val to = date.atTime(hour, minute).plusMinutes(45)

            val arrival = l.arrival

            val departure = l.departure

            val platform = l.platform
            val loc = locationsService.findLocationByTiploc(l.tiploc)
            DisplaySimpleTrainTimetableCallingPoint(
              loc.map(_.name).getOrElse(""),
              arrival map TimetableHelper.time getOrElse "",
              if (arrival.isDefined) "Arr." else "",
              departure map TimetableHelper.time getOrElse "",
              if (departure.isDefined) "Dep." else "",
              platform,
              if (platform != "") "Platform" else "",
              LocationTimetableServiceUrlHelper.createUrlForDisplayingLocationSimpleTimetables(
                loc.map(_.id).getOrElse(""),
                date.getYear,
                date.getMonthValue,
                date.getDayOfMonth,
                from.getHour * 100 + from.getMinute,
                to.getHour * 100 + to.getMinute
              )

            )
        },
      tt.basicSchedule.trainUid
    )

  }
}

case class DisplaySimpleTrainTimetable(
                                        operator: String,
                                        origin: String,
                                        destination: String,
                                        runningOn: LocalDate,
                                        locations: List[DisplaySimpleTrainTimetableCallingPoint],
                                        uid: String
                                           ) {
  def day = runningOn.getDayOfMonth
  def month = runningOn.getMonth.getValue
  def year = runningOn.getYear
}

case class DisplaySimpleTrainTimetableCallingPoint(name: String, arrival: String, arrivalLabel: String, departure: String, departureLabel: String, platform: String, platformLabel: String, url: String)





