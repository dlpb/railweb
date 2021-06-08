package models.timetable.model.train

import java.time.{DayOfWeek, LocalDate, ZoneId}
import java.util.{Date, UUID}

case class IndividualTimetable(
                                basicSchedule: BasicSchedule,
                                basicScheduleExtraDetails: BasicScheduleExtraDetails,
                                locations: List[IndividualTimetableLocation],
                                associations: List[Association] = List.empty,
                                id: String = UUID.randomUUID().toString) {

  def calculateDates(start: LocalDate, end: LocalDate, m: Boolean, t: Boolean, w: Boolean, th: Boolean, f: Boolean, s: Boolean, su: Boolean, dates: List[LocalDate]): List[LocalDate] = {
    if(start.isAfter(end)) dates
    else {
      if(start.getDayOfWeek.equals(DayOfWeek.MONDAY) && m) calculateDates(start.plusDays(1), end, m, t, w, th, f, s, su, start :: dates)
      else if(start.getDayOfWeek.equals(DayOfWeek.TUESDAY) && t) calculateDates(start.plusDays(1), end, m, t, w, th, f, s, su, start :: dates)
      else if(start.getDayOfWeek.equals(DayOfWeek.WEDNESDAY) && w) calculateDates(start.plusDays(1), end, m, t, w, th, f, s, su, start :: dates)
      else if(start.getDayOfWeek.equals(DayOfWeek.THURSDAY) && th) calculateDates(start.plusDays(1), end, m, t, w, th, f, s, su, start :: dates)
      else if(start.getDayOfWeek.equals(DayOfWeek.FRIDAY) && f) calculateDates(start.plusDays(1), end, m, t, w, th, f, s, su, start :: dates)
      else if(start.getDayOfWeek.equals(DayOfWeek.SATURDAY) && s) calculateDates(start.plusDays(1), end, m, t, w, th, f, s, su, start :: dates)
      else if(start.getDayOfWeek.equals(DayOfWeek.SUNDAY) && su) calculateDates(start.plusDays(1), end, m, t, w, th, f, s, su, start :: dates)
      else calculateDates(start.plusDays(1), end, m, t, w, th, f, s, su, dates)
    }
  }

  def validAssociationDates(association: Association): List[LocalDate] = {
    val startDate: Date = association.startDate
    val endDate: Date = association.endDate
    val m = association.validMonday
    val t = association.validTuesday
    val w = association.validWednesday
    val th = association.validThursday
    val f = association.validFriday
    val s = association.validSaturday
    val su = association.validSunday

    calculateDates(
      startDate.toInstant.atZone(ZoneId.systemDefault()).toLocalDate,
      endDate.toInstant.atZone(ZoneId.systemDefault()).toLocalDate,
      m,
      t,
      w,
      th,
      f,
      s,
      su,
      List.empty[LocalDate])
  }

  def validTimetableDates: List[LocalDate] = {
    val startDate: Date = basicSchedule.runsFrom
    val endDate: Date = basicSchedule.runsTo
    val m = basicSchedule.validMonday
    val t = basicSchedule.validTuesday
    val w = basicSchedule.validWednesday
    val th = basicSchedule.validThursday
    val f = basicSchedule.validFriday
    val s = basicSchedule.validSaturday
    val su = basicSchedule.validSunday

    calculateDates(
      startDate.toInstant.atZone(ZoneId.systemDefault()).toLocalDate,
      endDate.toInstant.atZone(ZoneId.systemDefault()).toLocalDate,
      m,
      t,
      w,
      th,
      f,
      s,
      su,
      List.empty[LocalDate])
  }
}