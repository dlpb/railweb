package models.plan.timetable

import java.time.ZonedDateTime

object TimetableDateTimeHelper {

  def padMonth(month: Int) = if (month < 1) "01" else if (month < 10) s"0$month" else if (month > 12) "12" else s"$month"
  def padDay(day: Int) = if (day < 1) "01" else if (day < 10) s"0$day" else if (day > 31) "31" else s"$day"
  def padTime(time: Int) = if (time < 0) "0000" else if (time < 10) s"000$time" else if (time < 100) s"00$time" else if (time < 1000) s"0$time" else if (time > 2400) "2400" else s"$time"

  def from: ZonedDateTime = ZonedDateTime.now().minusMinutes(15)

  def to: ZonedDateTime = ZonedDateTime.now().plusMinutes(45)

  def hourMinute(time: Int) = {
    val hour = time / 100
    val minute = time % 100
    (hour, minute)
  }

}
