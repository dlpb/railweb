package models.plan.timetable.trains

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
    val ids = tt.locations.flatMap(l => locService.findFirstLocationByTiploc(l.tiploc).map(_.id)).mkString("%0D%0A")
    val url = controllers.plan.route.find.pointtopoint.routes.PointToPointRouteController.index().url //TODO FIXME
    url
  }

  def createUrlForReadingTrainTimetable(train: String, year: String, month: String, day: String) = s"http://railweb-timetables-java.herokuapp.com/timetables/train/$train?year=$year&month=$month&day=$day"

}
