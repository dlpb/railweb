package models.plan.timetable.trains

import models.location.LocationsService
import models.plan.timetable.TimetableDateTimeHelper
import models.timetable.model.train.IndividualTimetable

object TrainTimetableServiceUrlHelper {
  def createUrlForDisplayingTrainSimpleTimetable(uid: String, year: Int, month: Int, day: Int) = {
    val m = TimetableDateTimeHelper.padMonth(month)
    val d = TimetableDateTimeHelper.padDay(day)
    val url = s"/plan/timetables/train/$uid/simple/$year/$m/$d"
    url
  }

  def createUrlForDisplayingDetailedTrainTimetable(uid: String, year: Int, month: Int, day: Int) = {
    val m = TimetableDateTimeHelper.padMonth(month)
    val d = TimetableDateTimeHelper.padDay(day)
    val url = s"/plan/timetables/train/$uid/detailed/$year/$m/$d"
    url
  }

  def buildRouteLink(tt: IndividualTimetable, locService: LocationsService): String = {
    val ids = tt.locations.flatMap(l => locService.findLocation(l.tiploc).map(_.id)).mkString("%0D%0A")
    val url = s"/plan/routes/point-to-point/find?followFixedLinks=false&followFreightLinks=true&waypoints=$ids"
    url
  }
}
