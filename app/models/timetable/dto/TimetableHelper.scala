package models.timetable.dto

import java.time.ZonedDateTime

object TimetableHelper {
  def time(time: Int): String = {
    if(time < 10) s"000$time"
    else if (time < 100) s"00$time"
    else if (time < 1000) s"0$time"
    else s"$time"
  }

  def defaultDate = {
    val now = ZonedDateTime.now
    val defaultDate = s"${now.getYear}-${now.getMonthValue}-${now.getDayOfMonth}"
    defaultDate
  }

}

