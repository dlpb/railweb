package models.timetable.dto.timetable.simple

import java.time.LocalDate

import models.location.LocationsService
import models.plan.timetable.TimetableService
import models.plan.trains.LocationTrainService
import models.timetable.dto.TimetableHelper
import models.timetable.model.train.IndividualTimetable

object DisplaySimpleIndividualTimetable {


  def apply(locationsService: LocationsService, tt: IndividualTimetable, year: Int, month: Int, day: Int): DisplaySimpleIndividualTimetable = {
    val date = LocalDate.of(year, month, day)

    DisplaySimpleIndividualTimetable(
      tt.basicScheduleExtraDetails.atocCode,
      tt.locations.headOption.flatMap(l => locationsService.findLocation(l.tiploc).map(l => l.name)).getOrElse(""),
      tt.locations.lastOption.flatMap(l => locationsService.findLocation(l.tiploc).map(l => l.name)).getOrElse(""),
      date,
      tt.locations
        .filter {
          l => l.pass.isEmpty && (l.publicDeparture.isDefined || l.publicArrival.isDefined)
        }
        .map {
          l =>
            val (hour, minute) = if (l.pass.isDefined) TimetableService.hourMinute(l.pass.get)
            else if (l.publicArrival.isDefined) TimetableService.hourMinute(l.publicArrival.get)
            else if (l.publicDeparture.isDefined) TimetableService.hourMinute(l.publicDeparture.get)
            else (0, 0)

            val from = date.atTime(hour, minute).minusMinutes(15)
            val to = date.atTime(hour, minute).plusMinutes(45)

            val arrival = l.arrival

            val departure = l.departure

            val platform = l.platform
            val loc = locationsService.findLocation(l.tiploc)
            DisplaySimpleIndividualTimetableLocation(
              loc.map(_.name).getOrElse(""),
              arrival map TimetableHelper.time getOrElse "",
              if (arrival.isDefined) "Arr." else "",
              departure map TimetableHelper.time getOrElse "",
              if (departure.isDefined) "Dep." else "",
              platform,
              if (platform != "") "Platform" else "",
              LocationTrainService.createUrlForDisplayingLocationSimpleTimetables(
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

case class DisplaySimpleIndividualTimetable(
                                           operator: String,
                                           origin: String,
                                           destination: String,
                                           runningOn: LocalDate,
                                           locations: List[DisplaySimpleIndividualTimetableLocation],
                                           uid: String
                                           ) {
  def day = runningOn.getDayOfMonth
  def month = runningOn.getMonth.getValue
  def year = runningOn.getYear
}

case class DisplaySimpleIndividualTimetableLocation(name: String, arrival: String, arrivalLabel: String, departure: String, departureLabel: String, platform: String, platformLabel: String, url: String)




