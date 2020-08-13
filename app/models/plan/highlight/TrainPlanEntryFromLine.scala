package models.plan.highlight

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime}

import models.location.LocationsService
import models.plan.highlight

object TrainPlanEntryFromLine {
  def apply(line: String)(implicit locationsService: LocationsService): Option[TrainPlanEntry] = {
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

      val boardLocation = locationsService.findPriortiseOrrStations(boardLocationCrs)
      val alightLocation = locationsService.findPriortiseOrrStations(alightCrs)
      val callingPoints = calledAt.split(",").toList.flatMap(l => locationsService.findPriortiseOrrStations(l))

      if(boardLocation.isEmpty || alightLocation.isEmpty) None

      Some(highlight.TrainPlanEntry(
        boardDate,
        boardLocation.get,
        boardTime,
        boardPlatform,
        alightLocation.get,
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
