package models.plan.highlight

import java.time.{LocalDate, LocalTime}

import models.location.Location

case class TrainPlan(entries: List[TrainPlanEntry], highlight: HighlightLocationsInTrainPlan)
case class HighlightLocationsInTrainPlan(srs: List[String], locations: List[String])

case class TrainPlanEntry(
                           boardDate: LocalDate,
                           boardLocationTiploc: String,
                           boardLocation: Location,
                           boardTime: LocalTime,
                           boardPlatform: String,
                           alightLocationTiploc: String,
                           alightLocation: Location,
                           alightTime: LocalTime,
                           alightPlatform: String,
                           trainId: String,
                           callingPoints: List[Location],
                           comments: String
) {
  def alightDate: LocalDate = {
    if(alightTime.isBefore(boardTime)) {
      boardDate.plusDays(1)
    }
    else boardDate
  }
  override def toString = {

    val railwebUrl = s"http://railweb.herokuapp.com/plan/timetables/train/${trainId}/detailed/${boardDate.getYear}/${boardDate.getMonth.getValue}/${boardDate.getDayOfMonth}"
    val sanitisedComments = if(comments.isBlank) railwebUrl else
      s"""
         |$comments
         |""".stripMargin.mkString(" ")

    val boardTimeFormatString = f"${boardTime}%4s"
    val alightTimeFormatString = f"${alightTime}%4s"
    val boardCrsFormatString = f"${boardLocation.crs.headOption.getOrElse(boardLocation.id)}%7s"
    val alightCrsFormatString = f"${alightLocation.crs.headOption.getOrElse(alightLocation.id)}%7s"
    val boardPlatformFormatString = if(boardPlatform.isBlank) "   -" else f"$boardPlatform%4s"
    val alightPlatformFormatString = if(alightPlatform.isBlank) "   -" else f"$alightPlatform%4s"
    val calledAtCrs = callingPoints.map(l => l.crs.headOption.getOrElse(l.id)).mkString(",")

    s"$boardDate $boardTimeFormatString $boardCrsFormatString $boardPlatformFormatString $alightPlatformFormatString $alightCrsFormatString $alightTimeFormatString $alightDate $trainId https://www.realtimetrains.co.uk/train/${trainId}/$boardDate/detailed $calledAtCrs ## $sanitisedComments"
  }
}
