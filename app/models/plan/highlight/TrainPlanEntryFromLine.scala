package models.plan.highlight

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime}

import models.location.LocationsService
import models.plan.highlight

object TrainPlanEntryFromLine {
  def apply(line: String)(implicit locationsService: LocationsService): Option[TrainPlanEntry] = {
    if(!line.startsWith("#")) None

    val lineParts = line.split(" ").toList.filterNot(_.isBlank)
    if(lineParts.size != 10) None


    try {
      val boardDate = LocalDate.parse(lineParts(0))
      val boardTime = LocalTime.parse(lineParts(1), DateTimeFormatter.ofPattern("HH:mm"))
      val boardLocationCrs = lineParts(2)
      val boardPlatform = lineParts(3)
      val alightPlatform = lineParts(4)
      val alightCrs = lineParts(5)
      val alightTIme = LocalTime.parse(lineParts(6), DateTimeFormatter.ofPattern("HH:mm"))
      val alightDate =  LocalDate.parse(lineParts(7))
      val trainId = lineParts(8)
      val rttUrl = lineParts(9)
      val calledAt = lineParts(10)
      val comments = lineParts(11)

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
        e.printStackTrace()
        None
    }
  }
}
