package models.timetable.dto.location

import java.time.LocalTime
import java.time.format.DateTimeFormatter

import models.plan.timetable.TimetableDateTimeHelper

trait SortableDisplayTimetable extends Ordered[SortableDisplayTimetable] {
  def arrival: String
  def departure: String

  override def compareTo(that: SortableDisplayTimetable): Int = {
    val thisTimeStr = TimetableDateTimeHelper.padTime((if (departure.isBlank) arrival else departure).toInt)
    val thatTimeStr = TimetableDateTimeHelper.padTime((if (that.departure.isBlank) that.arrival else that.departure).toInt)

    val thisTime = LocalTime.parse(thisTimeStr, DateTimeFormatter.ofPattern("HHmm"))
    val thatTime = LocalTime.parse(thatTimeStr, DateTimeFormatter.ofPattern("HHmm"))

    thisTime.compareTo(thatTime)
  }

  override def compare(that: SortableDisplayTimetable): Int = compareTo(that)
}
