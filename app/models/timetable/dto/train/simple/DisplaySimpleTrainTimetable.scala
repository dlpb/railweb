package models.timetable.dto.train.simple

import java.time.LocalDate

import models.plan.timetable.location.LocationTimetableServiceUrlHelper
import models.timetable.model.train.IndividualTimetable
import services.location.LocationService

object DisplaySimpleTrainTimetable {


  def apply(locationsService: LocationService, tt: IndividualTimetable, year: Int, month: Int, day: Int): DisplaySimpleTrainTimetable = {
    val date = LocalDate.of(year, month, day)

    DisplaySimpleTrainTimetable(
      tt.basicScheduleExtraDetails.atocCode,
      tt.locations.headOption.flatMap(l => locationsService.findFirstLocationByTiploc(l.tiploc).map(l => l.name)).getOrElse(""),
      tt.locations.lastOption.flatMap(l => locationsService.findFirstLocationByTiploc(l.tiploc).map(l => l.name)).getOrElse(""),
      date,
      tt.locations
        .filter {
          l => l.pass.isEmpty && (l.publicDeparture.isDefined || l.publicArrival.isDefined)
        }
        .map {
          l =>
            val (hour, minute) = if (l.pass.isDefined) (l.pass.get.getHour, l.pass.get.getMinute)
            else if (l.publicArrival.isDefined) (l.publicArrival.get.getHour, l.publicArrival.get.getMinute)
            else if (l.publicDeparture.isDefined) (l.publicDeparture.get.getHour, l.publicDeparture.get.getMinute)
            else (0, 0)

            val from = date.atTime(hour, minute).minusMinutes(15)
            val to = date.atTime(hour, minute).plusMinutes(45)

            val arrival = l.arrival

            val departure = l.departure

            val platform = l.platform
            val loc = locationsService.findFirstLocationByTiploc(l.tiploc)
            DisplaySimpleTrainTimetableCallingPoint(
              loc.map(_.name).getOrElse(""),
              arrival.map(_.toString).getOrElse(""),
              if (arrival.isDefined) "Arr." else "",
              departure.map(_.toString).getOrElse(""),
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





