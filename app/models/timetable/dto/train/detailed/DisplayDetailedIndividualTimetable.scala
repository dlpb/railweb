package models.timetable.dto.train.detailed

import java.time.{LocalDate, ZoneId}
import java.util.Date

import models.location.LocationsService
import models.plan.timetable.TimetableDateTimeHelper
import models.plan.timetable.location.{LocationTimetableFilters, LocationTimetableServiceUrlHelper}
import models.timetable.dto.TimetableHelper
import models.timetable.model.train.IndividualTimetable

object DisplayDetailedIndividualTimetable {
  def apply(locationsService: LocationsService, tt: IndividualTimetable, year: Int, month: Int, day: Int) : DisplayDetailedIndividualTimetable = {
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
      tt.basicSchedule.operatingCharacteristics.map(c=>c.toString).toList,
      tt.basicSchedule.seating.toString,
      tt.basicSchedule.sleepers.toString,
      tt.basicSchedule.reservations.toString,
      tt.basicSchedule.catering.map(c => c.toString).toList ,
      tt.basicSchedule.branding,
      tt.basicSchedule.stpIndicator.toString,
      tt.locations map {
        l =>
          val loc = locationsService.findLocation(l.tiploc)
          val isPass = l.pass.isDefined

          val public = LocationTimetableFilters.isPublicCategory(tt.basicSchedule.trainCategory)

          val arrival: String =
            if(l.pass.isDefined) "pass"
            else if(!public && l.arrival.isDefined)
              s"${l.arrival.map(TimetableHelper.time).getOrElse("")}${if(l.arrivalHalfMinute.isDefined) "½" else ""}"
            else if(l.publicArrival.isDefined) l.publicArrival.map(TimetableHelper.time).getOrElse("")
            else ""

          val departure =
            if(l.pass.isDefined) l.pass.map(TimetableHelper.time).getOrElse("")
            else if(!public && l.departure.isDefined)
              s"${l.departure.map(TimetableHelper.time).getOrElse("")}${if(l.departureHalfMinute.isDefined) "½" else ""}"
            else if(l.publicDeparture.isDefined) l.publicDeparture.map(TimetableHelper.time).getOrElse("")
            else ""

          val platform =
            if(l.pass.isDefined) ""
            else l.platform

          val pathAllowance = s"${l.pathingAllowance}${if(l.pathingAllowanceHalfMinute && l.pathingAllowanceHalfMinute) "½" else ""}"
          val performanceAllowance = s"${l.performanceAllowance}${if(l.performanceAllowanceHalfMinute && l.performanceAllowanceHalfMinute) "½" else ""}"
          val engineeringAllowance = s"${l.engineeringAllowance}${if(l.engineeringAllowanceHalfMinute && l.engineeringAllowanceHalfMinute) "½" else ""}"

          val (hour, minute) = if(l.pass.isDefined) TimetableDateTimeHelper.hourMinute(l.pass.get)
          else if (l.publicArrival.isDefined) TimetableDateTimeHelper.hourMinute(l.publicArrival.get)
          else if (l.publicDeparture.isDefined) TimetableDateTimeHelper.hourMinute(l.publicDeparture.get)
          else (0,0)

          val from = date.atTime(hour, minute).minusMinutes(15)
          val to = date.atTime(hour, minute).plusMinutes(45)

          DisplayDetailedTrainTimetableCallingPoint(
            loc.map(_.id).getOrElse(l.tiploc),
            loc.map(_.name).getOrElse(l.tiploc),
            platform,
            isPass,
            arrival,
            departure,
            if(pathAllowance == "0") "-" else pathAllowance,
            if(performanceAllowance == "0") "-" else performanceAllowance,
            if(engineeringAllowance == "0") "-" else engineeringAllowance,
            l.path,
            l.line,
            LocationTimetableServiceUrlHelper.createUrlForDisplayingLocationDetailedTimetables(
              loc.map(_.id).getOrElse(""),
              year,
              month,
              day,
              from.getHour*100+from.getMinute,
              to.getHour*100 + to.getMinute),
              loc.map(_.crs.mkString(" ")).getOrElse("")
          )
      }


    )
  }
}

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
                                             locations: List[DisplayDetailedTrainTimetableCallingPoint]
                                             ) {
  def day = runningOn.getDayOfMonth
  def month = runningOn.getMonth.getValue
  def year = runningOn.getYear
  def from = runsFrom.toInstant.atZone(ZoneId.of("Z")).toLocalDate
  def to = runsTo.toInstant.atZone(ZoneId.of("Z")).toLocalDate
}

case class DisplayDetailedTrainTimetableCallingPoint(
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
                                                     url: String,
                                                     crs: String
                                                     )