package models.timetable

import java.time.LocalDate
import java.util.Date

import models.location.LocationsService
import models.plan.PlanService
import DisplayTimetable._

class DisplayTimetable(locationsService: LocationsService, planService: PlanService) {
  def displayIndividualTimetable(tt: IndividualTimetable): DisplaySimpleIndividualTimetable = {
    val date = LocalDate.now()
    DisplaySimpleIndividualTimetable(
      tt.basicScheduleExtraDetails.atocCode,
      tt.locations.headOption.flatMap(l => locationsService.findLocation(l.tiploc).map(l => l.name)).getOrElse(""),
      tt.locations.lastOption.flatMap(l => locationsService.findLocation(l.tiploc).map(l => l.name)).getOrElse(""),
      date,
      tt.locations
        .filter{
          l => l.pass.isEmpty && (l.publicDeparture.isDefined || l.publicArrival.isDefined)
        }
        .map {
        l =>
          val (hour, minute) = if(l.pass.isDefined) PlanService.hourMinute(l.pass.get)
          else if (l.publicArrival.isDefined) PlanService.hourMinute(l.publicArrival.get)
          else if (l.publicDeparture.isDefined) PlanService.hourMinute(l.publicDeparture.get)
          else (0,0)

          val from = date.atTime(hour, minute).minusMinutes(15)
          val to = date.atTime(hour, minute).plusMinutes(45)

          val arrival = if(l.isInstanceOf[LocationIntermediate]) {
            l.publicArrival.map(time).getOrElse(l.publicDeparture.map(time).getOrElse(""))
          } else { l.publicArrival.map(time).getOrElse("") }

          val departure = if(l.isInstanceOf[LocationIntermediate]) {
            l.publicDeparture.map(time).getOrElse(l.publicArrival.map(time).getOrElse(""))
          } else {l.publicDeparture.map(time).getOrElse("") }

          val platform = l.platform
          DisplaySimpleTimetableLocation(
            locationsService.findLocation(l.tiploc).map(_.name).getOrElse(""),
            arrival,
            if(arrival != "") "Arr." else "",
            departure,
            if(departure != "") "Dep." else "",
            platform,
            if(platform != "") "Platform" else "",
            PlanService.createUrlForDisplayingLocationSimpleTimetables(
              locationsService.findLocation(l.tiploc).map(_.id).getOrElse(""),
              date.getYear,
              date.getMonthValue,
              date.getDayOfMonth,
              from.getHour*100+from.getMinute,
              to.getHour*100 + to.getMinute
            )


          )
      }
    )
  }

  def displaySimpleTimetable(simpleTimetable: SimpleTimetable, year: Int,  month: Int, day: Int): DisplaySimpleTimetable2 = {
    val arrival = simpleTimetable.location.publicArrival.map(time).getOrElse("")
    val departure = simpleTimetable.location.publicDeparture.map(time).getOrElse("")
    val platform = simpleTimetable.location.platform
    DisplaySimpleTimetable2(
      arrival,
      departure,
      locationsService.findLocation(simpleTimetable.origin.tiploc).map(_.name).getOrElse(simpleTimetable.origin.tiploc),
      locationsService.findLocation(simpleTimetable.destination.tiploc).map(_.name).getOrElse(simpleTimetable.destination.tiploc),
      platform,
      PlanService.createUrlForDisplayingSimpleTrainTimetable(simpleTimetable.basicSchedule.trainUid, year, month, day),
      if(arrival != "") "Arr." else "",
      if(departure != "") "Dep." else "",
      if(platform != "") "Platform" else ""
    )
  }
}
object DisplayTimetable {
  def time(time: Int): String = {
    if(time < 10) s"000$time"
    else if (time < 100) s"00$time"
    else if (time < 1000) s"0$time"
    else s"$time"
  }
}

case class DisplaySimpleTimetable2(
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

case class DisplaySimpleIndividualTimetable(
                                           operator: String,
                                           origin: String,
                                           destination: String,
                                           runningOn: LocalDate,
                                           locations: List[DisplaySimpleTimetableLocation]
                                           )

case class DisplaySimpleTimetableLocation(
                                label: String,
                                arrival: String,
                                arrivalLabel: String,
                                departure: String,
                                departureLabel: String,
                                platform: String,
                                platformLabel: String,
                                url: String
                                )