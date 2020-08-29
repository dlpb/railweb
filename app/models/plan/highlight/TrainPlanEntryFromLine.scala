package models.plan.highlight

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime}

import models.location
import models.location.LocationsService
import models.plan.highlight
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.model.train.{IndividualTimetable, Location}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object TrainPlanEntryFromLine {
  def apply(line: String)(implicit locationsService: LocationsService, trainTimetableService: TrainTimetableService): TrainPlanEntryParseResult = {
    if (line.startsWith("#")) TrainPlanEntryParseResult(None, List.empty)
    else {

      val lineParts = line.split(" ").toList.filterNot(_.isBlank)
      if(lineParts.size == 10) {
        //likely format as presented by the writer for Train Plan Entry
        // can safely split on space
        //example
        //2020-10-20 1616 HFX    1    2 ELP 1808 https://www.realtimetrains.co.uk/train/L17825/2020-10-19/detailed HFX,SOW,MYT,HBD,TOD,RCD,MCV,WBQ,RUE,FRD,HSB,INE,SNT,ELP
        extractFullTimetablePlanLine(trainTimetableService, locationsService, lineParts)
      }
      else {
        //likely old style format
        //need to do fixed width splitting apart from the calling points
        // example
        // 2020 10 20 1616 HFX    1    2 ELP 1808 https://www.realtimetrains.co.uk/train/L17825/2020-10-19/detailed HFX,SOW,MYT,HBD,TOD,RCD,MCV,WBQ,RUE,FRD,HSB,INE,SNT,ELP
        extractTimetablePlanEntryWithFixedWidthFormatting(trainTimetableService, locationsService, line)
      }
    }
  }


  private def extractTimetablePlanEntryWithFixedWidthFormatting(trainTimetableService: TrainTimetableService, locationsService: LocationsService, line: String): TrainPlanEntryParseResult = {

    try {
      val boardDateStr = line.substring(0, 10)
      val boardTimeStr = makeFormat(line.substring(11, 15))
      val boardLocationCrs = line.substring(16,19)
      val boardPlatform = line.substring(21,24)
      val alightPlatform = line.substring(26, 29)
      val alightCrs = line.substring(30,33)
      val alightTImeStr = makeFormat(line.substring(34,38))
      val rttUrl = line.substring(39,104)
      val calledAt = line.substring(105)
      val trainId = rttUrl.split("/")(4)

      val boardDate = LocalDate.parse(boardDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
      val boardTime = LocalTime.parse(boardTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
      val alightTIme = LocalTime.parse(alightTImeStr, DateTimeFormatter.ofPattern("HH:mm"))

      getTrainPlanEntryResult(trainTimetableService, locationsService, boardLocationCrs, boardPlatform, alightPlatform, alightCrs, trainId, calledAt, "", boardDate, boardTime, alightTIme)

    }
    catch {
      case e: Exception =>
        val errorMsg = s"""Something went wrong processing line: '${line}'. ${e.getMessage}. Caused by: ${e.getClass}"""
        println(errorMsg)
        e.printStackTrace()
        TrainPlanEntryParseResult(None, List(new IllegalArgumentException(errorMsg, e)))
    }
  }

  private def extractFullTimetablePlanLine(trainTimetableService: TrainTimetableService, locationsService: LocationsService, lineParts: List[String]): TrainPlanEntryParseResult = {


    if(lineParts != 10) {
      val error = new IllegalArgumentException(s"could not parse line ${lineParts.mkString(" ")}. It did not have the right number of fields. Expected format: boardDate boardTime boardCrs boardPlatform alightPlatform alightCrs alightTime alightDate trainId realTimeTrainsUrl calledAt ## comments")
      return TrainPlanEntryParseResult(None, List(error))
    }

    try {
      val boardDateStr = lineParts(0)
      val boardTimeStr = makeFormat(lineParts(1))
      val boardLocationCrs = lineParts(2)
      val boardPlatform = lineParts(3)
      val alightPlatform = lineParts(4)
      val alightCrs = lineParts(5)
      val alightTImeStr = makeFormat(lineParts(6))
      val alightDateStr = lineParts(7)
      val trainId = lineParts(8)
      val rttUrl = if (lineParts.size > 9) lineParts(9) else ""
      val calledAt = if (lineParts.size > 10) lineParts(10) else ""
      val comments = ""

      val boardDate = LocalDate.parse(boardDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
      val boardTime = LocalTime.parse(boardTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
      val alightTIme = LocalTime.parse(alightTImeStr, DateTimeFormatter.ofPattern("HH:mm"))
      val alightDate = LocalDate.parse(alightDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))

      getTrainPlanEntryResult(trainTimetableService, locationsService, boardLocationCrs, boardPlatform, alightPlatform, alightCrs, trainId, calledAt, comments, boardDate, boardTime, alightTIme)

    }
    catch {
      case e: Exception =>
        val errorMsg = s"""Something went wrong processing line: '${lineParts.mkString}'. ${e.getMessage}. Caused by: ${e.getClass}"""
        println(errorMsg)
        TrainPlanEntryParseResult(None, List(new IllegalArgumentException(errorMsg, e)))
    }
  }

  private def getTrainPlanEntryResult(
                                       trainTimetableService: TrainTimetableService,
                                       locationsService: LocationsService,
                                       boardLocationCrs: String,
                                       boardPlatform: String,
                                       alightPlatform: String,
                                       alightCrs: String,
                                       trainId: String,
                                       calledAt: String,
                                       comments: String,
                                       boardDate: LocalDate,
                                       boardTime: LocalTime,
                                       alightTIme: LocalTime) = {
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

    val boardLocation = if(boardTiplocFromTimetable.isDefined) boardTiplocFromTimetable.get._2 else locationsService.findAllLocationsMatchingCrs(boardLocationCrs).head
    val boardLocationTiploc = if(boardTiplocFromTimetable.isDefined) boardTiplocFromTimetable.get._1 else locationsService.findAllLocationsMatchingCrs(boardLocationCrs).map(_.id).headOption.getOrElse(boardLocationCrs)
    val alightLocation = if(alightTiplocFromTimetable.isDefined) alightTiplocFromTimetable.get._2 else locationsService.findAllLocationsMatchingCrs(alightCrs).head
    val alightLocationTiploc = if(alightTiplocFromTimetable.isDefined) alightTiplocFromTimetable.get._1 else locationsService.findAllLocationsMatchingCrs(alightCrs).map(_.id).headOption.getOrElse(boardLocationCrs)

    val boardAlightError: Option[IllegalArgumentException] = if (boardTiplocFromTimetable.isEmpty || alightTiplocFromTimetable.isEmpty) {
      val errorMsg = s"Could not find board location for $boardLocationCrs or alight location for $alightCrs in train $trainId on $boardDate with calling points ${callingPointsFromTimetable.map(c => s"${c._2.name}[${c._2.id}] [${c._2.crs}]")}"
      println(errorMsg)
      Some(new IllegalArgumentException(errorMsg))
    } else None

    val entry = Some(highlight.TrainPlanEntry(
      boardDate,
      boardLocationTiploc,
      boardLocation,
      boardTime,
      boardPlatform,
      alightLocationTiploc,
      alightLocation,
      alightTIme,
      alightPlatform,
      trainId,
      callingPoints,
      comments
    ))

    val result = TrainPlanEntryParseResult(entry, boardAlightError.toList)
    result
  }

  def makeFormat(str: String): String = if (!str.contains(":")) {
    val strSplit = str.splitAt(2)
    strSplit._1 + ":" + strSplit._2
  } else str
}

case class TrainPlanEntryParseResult(entry: Option[TrainPlanEntry], errors: List[Throwable])
