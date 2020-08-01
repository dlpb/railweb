package models.plan.timetable.location

import models.plan.timetable.TimetableDateTimeHelper

object LocationTimetableServiceUrlHelper {
  def createUrlForReadingLocationTimetables(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int) = {
    val m = TimetableDateTimeHelper.padMonth(month)
    val d = TimetableDateTimeHelper.padDay(day)
    val f = TimetableDateTimeHelper.padTime(from)
    val t = TimetableDateTimeHelper.padTime(to)
    val url = s"http://railweb-timetables-java.herokuapp.com/timetables/location/$loc?year=$year&month=$m&day=$d&from=$f&to=$t"
    url
  }

  def createUrlForDisplayingLocationSimpleTimetables(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int) = {
    val url = controllers.plan.timetable.location.simple.routes.SimpleLocationTimetableController.index(loc,year,month,day,from,to).url
    url
  }

  def createUrlForDisplayingLocationDetailedTimetables(loc: String, year: Int, month: Int, day: Int, from: Int, to: Int) = {
    val url = controllers.plan.timetable.location.detailed.routes.DetailedLocationTimetableController.index(loc,year,month,day,from,to).url
    url
  }

}
