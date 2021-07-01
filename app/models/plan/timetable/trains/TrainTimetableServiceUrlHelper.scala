package models.plan.timetable.trains

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import models.plan.timetable.TimetableDateTimeHelper
import models.timetable.model.train.IndividualTimetable
import services.location.LocationService

object TrainTimetableServiceUrlHelper {
  def createUrlForDisplayingTrainSimpleTimetable(uid: String, year: Int, month: Int, day: Int) = {
    val url = controllers.plan.timetable.train.simple.routes.SimpleTrainTimetableController.index(uid, year, month, day).url
    url
  }

  def createUrlForDisplayingDetailedTrainTimetable(uid: String, year: Int, month: Int, day: Int) = {
    val m = TimetableDateTimeHelper.padMonth(month)
    val d = TimetableDateTimeHelper.padDay(day)
    val url = controllers.plan.timetable.train.detailed.routes.DetailedTrainTimetableController.index(uid, year, month, day).url
    url
  }

  def buildRouteLink(tt: IndividualTimetable, locService: LocationService): String = {
    val url = controllers.plan.route.find.result.timetable.routes.TimetableFindRouteResultController.timetable(tt.basicSchedule.trainUid, LocalDate.now.format(DateTimeFormatter.ISO_LOCAL_DATE)).url
    url
  }

  def createUrlForReadingTrainTimetable(train: String, year: String, month: String, day: String) = s"http://railweb-timetables-java.herokuapp.com/timetables/train/$train?year=$year&month=$month&day=$day"

}
