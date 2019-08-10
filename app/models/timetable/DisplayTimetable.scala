package models.timetable

import java.time.LocalDate
import java.util.Date

import models.location.LocationsService
import models.plan.PlanService
import DisplayTimetable._

class DisplayTimetable(locationsService: LocationsService, planService: PlanService) {
  def displayDetailedIndividualTimetable(tt: IndividualTimetable, year: Int, month: Int, day: Int) : DisplayDetailedIndividualTimetable = {
    val date = LocalDate.of(year, month, day)
    val m = if(tt.basicSchedule.validMonday) "M" else ""
    val t = if(tt.basicSchedule.validTuesday) "T" else ""
    val w = if(tt.basicSchedule.validWednesday) "W" else ""
    val th = if(tt.basicSchedule.validThursday) "Th" else ""
    val f = if(tt.basicSchedule.validFriday) "F" else ""
    val s = if(tt.basicSchedule.validSaturday) "S" else ""
    val su = if(tt.basicSchedule.validSunday) "Su" else ""
    val runningDays = s"$m$t$w$th$f$s$su"

    DisplayDetailedIndividualTimetable(
      tt.locations.headOption.flatMap(l => locationsService.findLocation(l.tiploc).map(l => l.name)).getOrElse(""),
      tt.locations.lastOption.flatMap(l => locationsService.findLocation(l.tiploc).map(l => l.name)).getOrElse(""),
      tt.basicScheduleExtraDetails.atocCode,
      tt.basicSchedule.trainIdentity,
      tt.basicSchedule.trainUid,
      date,
      tt.basicSchedule.runsFrom,
      tt.basicSchedule.runsTo,
      runningDays,
      tt.basicSchedule.bankHolidayRunning.toString,
      tt.basicSchedule.trainCategory.toString,
      tt.basicSchedule.timing.toString,
      tt.basicSchedule.trainStatus.toString,
      tt.basicSchedule.powerType.toString,
      tt.basicSchedule.speed.toString,
      tt.basicSchedule.operatingCharacteristics.map(c=>c.toString),
      tt.basicSchedule.seating.toString,
      tt.basicSchedule.sleepers.toString,
      tt.basicSchedule.reservations.toString,
      tt.basicSchedule.catering.map(c => c.toString) ,
      tt.basicSchedule.branding,
      tt.basicSchedule.stpIndicator.toString,
      tt.locations map {
        l =>
          val loc = locationsService.findLocation(l.tiploc)
          val isPass = l.pass.isDefined

          val public = PlanService.isPublicCategory(tt.basicSchedule.trainCategory)

          val arrival: String =
            if(l.pass.isDefined) "pass"
            else if(!public && l.arrival.isDefined)
              s"${l.arrival.map(time).getOrElse("")}${if(l.arrivalHalfMinute.isDefined) "½" else ""}"
            else if(l.publicArrival.isDefined) l.publicArrival.map(time).getOrElse("")
            else ""

          val departure =
            if(l.pass.isDefined) l.pass.map(time).getOrElse("")
            else if(!public && l.departure.isDefined)
              s"${l.departure.map(time).getOrElse("")}${if(l.departureHalfMinute.isDefined) "½" else ""}"
            else if(l.publicDeparture.isDefined) l.publicDeparture.map(time).getOrElse("")
            else ""

          val platform =
            if(l.pass.isDefined) ""
            else l.platform

          val pathAllowance = s"${l.pathingAllowance}${if(l.pathingAllowanceHalfMinute) "½" else ""}"
          val performanceAllowance = s"${l.performanceAllowance}${if(l.performanceAllowanceHalfMinute) "½" else ""}"
          val engineeringAllowance = s"${l.engineeringAllowance}${if(l.engineeringAllowanceHalfMinute) "½" else ""}"

          val (hour, minute) = if(l.pass.isDefined) PlanService.hourMinute(l.pass.get)
          else if (l.publicArrival.isDefined) PlanService.hourMinute(l.publicArrival.get)
          else if (l.publicDeparture.isDefined) PlanService.hourMinute(l.publicDeparture.get)
          else (0,0)

          val from = date.atTime(hour, minute).minusMinutes(15)
          val to = date.atTime(hour, minute).plusMinutes(45)

          DisplayDetailedIndividualTimetableLocation(
            loc.map(_.id).getOrElse(l.tiploc),
            loc.map(_.name).getOrElse(l.tiploc),
            platform,
            isPass,
            arrival,
            departure,
            if(pathAllowance == "0") "" else pathAllowance,
            if(performanceAllowance == "0") "" else performanceAllowance,
            if(engineeringAllowance == "0") "" else engineeringAllowance,
            l.path.getOrElse(""),
            l.line,
            PlanService.createUrlForDisplayingLocationDetailedTimetables(
              loc.map(_.id).getOrElse(""),
              year,
              month,
              day,
              from.getHour*100+from.getMinute,
              to.getHour*100 + to.getMinute)
          )
      }


    )
  }

  def displayDetailedTimetable(simpleTimetable: SimpleTimetable, year: Int, month: Int, day: Int): DisplayDetailedTimetable = {
    val public = PlanService.isPassengerTrain(simpleTimetable)

    val arrival: String =
      if(simpleTimetable.location.pass.isDefined) "pass"
      else if(!public && simpleTimetable.location.arrival.isDefined) simpleTimetable.location.arrival.map(time).getOrElse("")
      else if(simpleTimetable.location.publicArrival.isDefined) simpleTimetable.location.publicArrival.map(time).getOrElse("")
      else ""

    val departure =
      if(simpleTimetable.location.pass.isDefined) simpleTimetable.location.pass.map(time).getOrElse("")
      else if(!public && simpleTimetable.location.departure.isDefined) simpleTimetable.location.departure.map(time).getOrElse("")
      else if(simpleTimetable.location.publicDeparture.isDefined) simpleTimetable.location.publicDeparture.map(time).getOrElse("")
      else ""

    val platform =
      if(simpleTimetable.location.pass.isDefined) ""
      else simpleTimetable.location.platform

    DisplayDetailedTimetable(
      public,
      simpleTimetable.location.pass.isDefined,
      simpleTimetable.basicSchedule.trainCategory.toString,
      simpleTimetable.basicSchedule.trainIdentity,
      simpleTimetable.basicSchedule.trainUid,
      arrival,
      departure,
      locationsService.findLocation(simpleTimetable.origin.tiploc).map(_.name).getOrElse(simpleTimetable.origin.tiploc),
      locationsService.findLocation(simpleTimetable.destination.tiploc).map(_.name).getOrElse(simpleTimetable.destination.tiploc),
      platform,
      PlanService.createUrlForDisplayingDetailedTrainTimetable(simpleTimetable.basicSchedule.trainUid, year, month, day),
      "",
      "",
      ""
    )

  }

  def displayIndividualTimetable(tt: IndividualTimetable,  year: Int, month: Int, day: Int): DisplaySimpleIndividualTimetable = {
    val date = LocalDate.of(year, month, day)
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
          val loc = locationsService.findLocation(l.tiploc)
          DisplaySimpleTimetableLocation(
            loc.map(_.name).getOrElse(""),
            arrival,
            if(arrival != "") "Arr." else "",
            departure,
            if(departure != "") "Dep." else "",
            platform,
            if(platform != "") "Platform" else "",
            PlanService.createUrlForDisplayingLocationSimpleTimetables(
              loc.map(_.id).getOrElse(""),
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

  def displaySimpleTimetable(simpleTimetable: SimpleTimetable, year: Int,  month: Int, day: Int): DisplaySimpleTimetable = {
    val arrival = simpleTimetable.location.publicArrival.map(time).getOrElse("")
    val departure = simpleTimetable.location.publicDeparture.map(time).getOrElse("")
    val platform = simpleTimetable.location.platform

    DisplaySimpleTimetable(
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

case class DisplaySimpleTimetable(
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

case class DisplayDetailedTimetable(
                                    isPublic: Boolean,
                                    isPass: Boolean,
                                    category: String,
                                    id: String,
                                    uid: String,
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

case class DisplayDetailedIndividualTimetable(
                                             origin: String,
                                             destination: String,
                                             operator: String,
                                             id: String,
                                             uid: String,
                                             runningOn: LocalDate,
                                             runsFrom: Date,
                                             runsTo: Date,
                                             runningDays: String,
                                             bankHolidayRunning: String,
                                             category: String,
                                             timing: String,
                                             status: String,
                                             powerType: String,
                                             speed: String,
                                             operatingCharacteristics: List[String],
                                             seating: String,
                                             sleepers: String,
                                             reservations: String,
                                             catering: List[String],
                                             branding: String,
                                             stpIndicator: String,
                                             locations: List[DisplayDetailedIndividualTimetableLocation]
                                             )

case class DisplayDetailedIndividualTimetableLocation(
                                                     id: String,
                                                     label: String,
                                                     platform: String,
                                                     isPass: Boolean,
                                                     arrival: String,
                                                     departure: String,
                                                     pathAllowance: String,
                                                     performanceAllowance: String,
                                                     engineeringAllowance: String,
                                                     path: String,
                                                     line: String,
                                                     url: String
                                                     )