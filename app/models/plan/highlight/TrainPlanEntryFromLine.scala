package models.plan.highlight

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime}

import models.location
import models.location.LocationsService
import models.plan.highlight
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.model.train.{IndividualTimetable, Location}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object TrainPlanEntryFromLine {
  def apply(line: String)(implicit locationsService: LocationsService, trainTimetableService: TrainTimetableService): Option[TrainPlanEntry] = {
    if(!line.startsWith("#")) None

    val lineParts = line.split(" ").toList.filterNot(_.isBlank)
    if(lineParts.size < 10) {
      println(s"Not enough fields for ${line}")
      None
    }

    try {
      val boardDateStr = lineParts(0)
      val boardTimeStr = lineParts(1)
      val boardLocationCrs = lineParts(2)
      val boardPlatform = lineParts(3)
      val alightPlatform = lineParts(4)
      val alightCrs = lineParts(5)
      val alightTImeStr = lineParts(6)
      val alightDateStr =  lineParts(7)
      val trainId = lineParts(8)
      val rttUrl = lineParts(9)
      val calledAt = if(lineParts.size > 10) lineParts(10) else ""
      val comments = ""

      val boardDate = LocalDate.parse(boardDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
      val boardTime = LocalTime.parse(boardTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
      val alightTIme = LocalTime.parse(alightTImeStr, DateTimeFormatter.ofPattern("HH:mm"))
      val alightDate =  LocalDate.parse(alightDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))


      val callingPoints = calledAt.split(",").toList.flatMap(l => locationsService.findLocationByNameTiplocCrsIdPrioritiseOrrStations(l))

      val timetableF: Future[Option[IndividualTimetable]] = trainTimetableService.getTrain(trainId, boardDate.getYear.toString, boardDate.getMonth.getValue.toString, boardDate.getDayOfMonth.toString)

      val timetable = Await.result(timetableF, Duration(30, "seconds"))

      val callingPointsFromTimetable: List[(String, location.Location)] = timetable
        .map(_.locations)
        .getOrElse(List.empty)
        .map(l => (l.tiploc, locationsService.findLocationByNameTiplocCrsIdPrioritiseOrrStations(l.tiploc).get))

      val boardTiplocFromTimetable = callingPointsFromTimetable.find(cp => {
        val crs = cp._2.crs
        crs contains boardLocationCrs
      })

      val alightTiplocFromTimetable = callingPointsFromTimetable.find(cp => {
        val crs = cp._2.crs
        crs contains alightCrs
      })

      val boardLocation = boardTiplocFromTimetable.get._2
      val alightLocation = alightTiplocFromTimetable.get._2

      if(boardTiplocFromTimetable.isEmpty || alightTiplocFromTimetable.isEmpty) println(s"Could not find board location for $boardLocationCrs or alight location for $alightCrs")

      Some(highlight.TrainPlanEntry(
        boardDate,
        boardTiplocFromTimetable.get._1,
        boardLocation,
        boardTime,
        boardPlatform,
        alightTiplocFromTimetable.get._1,
        alightLocation,
        alightTIme,
        alightPlatform,
        trainId,
        callingPoints,
        comments
      ))

    }
    catch {
      case e: Exception =>
        println(s"Something went wrong processing line ${line}")
        e.printStackTrace()
        None
    }
  }
}
