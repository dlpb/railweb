package models.plan.highlight

import java.time.{LocalDate, LocalTime}
import java.time.format.DateTimeFormatter
import java.util.UUID

import javax.inject.{Inject, Singleton}
import models.location.{Location, LocationsService, MapLocation}
import models.plan.highlight
import models.plan.highlight.FormKeyTypes.{KeyName, KeyValue, TypeOfKey}
import models.plan.timetable.TimetableDateTimeHelper
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.model.train.IndividualTimetable
import play.api.mvc.ControllerComponents

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

@Singleton
class HighlightTimetableService @Inject()(
                                           cc: ControllerComponents,
                                           locationsService: LocationsService,
                                           trainService: LocationTimetableService,
                                           timetableService: TrainTimetableService
                                         ) {

   def getTrainDataFromFormEntryGroupedByRow(data: Map[String, String]): Map[TypeOfKey, Map[KeyName, KeyValue]] = {
    val groupedDataByRow: Map[TypeOfKey, Map[KeyName, KeyValue]] = data
      .keys
      .groupBy(key => key.split("_").toList.last)
      .map(row => {
        row._1 -> row._2.map(key => key -> data(key)).toMap
      })
    groupedDataByRow
  }

  def makeReturnFormDataFromTrainPlans(trainPlans: List[TrainPlanEntry]): List[Map[TypeOfKey, (KeyName, KeyValue)]] = {
    trainPlans.map(plan => {
      val uuid = UUID.randomUUID()
      val locationKey = s"location_$uuid"
      val dateKey = s"date_$uuid"
      val trainKey = s"train-id_$uuid"
      val boardKey = s"board_$uuid"
      val alightKey = s"alight_$uuid"
      val wilLCallAtKey = s"willCallAt_$uuid"
      val hasCalledAtKey = s"hasCalledAt_$uuid"
      val date = plan.boardDate
      val trainId = plan.trainId
      val board = plan.boardLocation.id
      val alight = plan.alightLocation.id
      val location = plan.boardLocation.id
      val wilLCallAt = ""
      val hasCalledAt = ""
      val map = Map(
        "date" -> (dateKey, date.toString),
        "trainId" -> (trainKey, trainId),
        "board" -> (boardKey, board),
        "alight" -> (alightKey, alight),
        "location" -> (locationKey, location),
        "willCallAt" -> (wilLCallAtKey, wilLCallAt),
        "hasCalledAt" -> (hasCalledAtKey, hasCalledAt))
      map
    })
  }

   def makeReturnDataFromForm(groupedDataByRow: Map[TypeOfKey, Map[KeyName, KeyValue]]): List[Map[TypeOfKey, (KeyName, KeyValue)]] = {
    val formDataToReturn: List[Map[String, (String, String)]] = groupedDataByRow.map(entry => {
      val (row, data) = entry
      val locationKey = s"location_$row"
      val dateKey = s"date_$row"
      val trainKey = s"train-id_$row"
      val boardKey = s"board_$row"
      val alightKey = s"alight_$row"
      val wilLCallAtKey = s"willCallAt_$row"
      val hasCalledAtKey = s"hasCalledAt_$row"
      val date = data.getOrElse(dateKey, "")
      val trainId = data.getOrElse(trainKey, "")
      val board = data.getOrElse(boardKey, "")
      val alight = data.getOrElse(alightKey, "")
      val location = data.getOrElse(locationKey, "")
      val wilLCallAt = data.getOrElse(wilLCallAtKey, "")
      val hasCalledAt = data.getOrElse(hasCalledAtKey, "")
      val map = Map(
        "date" -> (dateKey, date),
        "trainId" -> (trainKey, trainId),
        "board" -> (boardKey, board),
        "alight" -> (alightKey, alight),
        "location" -> (locationKey, location),
        "willCallAt" -> (wilLCallAtKey, wilLCallAt),
        "hasCalledAt" -> (hasCalledAtKey, hasCalledAt))
      map
    }).toList
    formDataToReturn
  }

   def getTrainPlan(dataPlanEntriesF: Future[List[TrainPlanEntry]]) = {
    Await.result(dataPlanEntriesF, Duration(30, "seconds")).mkString("\n")
  }

   def getSortedTrainPlanEntries(trainDataPlanF: List[Future[Option[TrainPlanEntry]]])(implicit executionContext: ExecutionContext): Future[List[TrainPlanEntry]] = {
    val dataPlanEntriesF: Future[List[TrainPlanEntry]] = Future
      .sequence(trainDataPlanF)
      .map(_.flatten)
      .map(_.sortWith(TrainPlanSorter.apply))
    dataPlanEntriesF
  }

   def getTrainPlanEntriesFuture(locationsCalledAtF: List[Future[Option[LocationsCalledAtFromTimetable]]])(implicit executionContext: ExecutionContext): List[Future[Option[TrainPlanEntry]]] = {
    locationsCalledAtF.map(f =>
      f.map(o =>
        o.flatMap(l => {
          val date = l.date
          val timetable = l.timetable
          val boardTimetableEntry = timetable.locations.find(_.tiploc.equals(l.board))
          val alightTimetableEntry = timetable.locations.find(_.tiploc.equals(l.alight))
          val boardTime = TimetableDateTimeHelper.padTime(boardTimetableEntry.map(l => l.publicDeparture.getOrElse(l.departure.getOrElse(0))).getOrElse(0))
          val boardPlatform = boardTimetableEntry.map(_.platform).getOrElse("")
          val alightTime = TimetableDateTimeHelper.padTime(alightTimetableEntry.map(l => l.publicArrival.getOrElse(l.arrival.getOrElse(0))).getOrElse(0))
          val alightPlatform = alightTimetableEntry.map(_.platform).getOrElse("")
          val boardLocationOpt = boardTimetableEntry.flatMap(l => locationsService.findPriortiseOrrStations(l.tiploc))
          val alightLocationOpt = alightTimetableEntry.flatMap(l => locationsService.findPriortiseOrrStations(l.tiploc))
          val calledAtLocations = l.locations

          val result: Option[TrainPlanEntry] = (boardLocationOpt, alightLocationOpt) match {
            case (Some(boardLocation), Some(alightLocation)) =>
              Some(highlight.TrainPlanEntry(
                date,
                boardLocation.id,
                boardLocation,
                LocalTime.parse(boardTime, DateTimeFormatter.ofPattern("HHmm")),
                boardPlatform,
                alightLocation.id,
                alightLocation,
                LocalTime.parse(alightTime, DateTimeFormatter.ofPattern("HHmm")),
                alightPlatform,
                l.trainId,
                calledAtLocations,
                ""
              ))
            case _ => None
          }
          result
        })
      )
    )
  }

   def getMapLocationsForLocationsCalledAt(locationsCalledAtF: List[Future[Option[LocationsCalledAtFromTimetable]]])(implicit executionContext: ExecutionContext): List[MapLocation] = {
    val mapLocationsCalledAtF: Future[List[MapLocation]] = Future.sequence(locationsCalledAtF)
      .map(_.flatten)
      .map(_.flatMap(_.locations.map(MapLocation(_))))

    val mapLocationsCalledAt = Await.result(mapLocationsCalledAtF, Duration(30, "seconds"))
    mapLocationsCalledAt
  }

   def getLocationsCalledAtFuture(timetablesF: List[Future[Option[TimetableFound]]])(implicit executionContext: ExecutionContext): List[Future[Option[LocationsCalledAtFromTimetable]]] = {
    timetablesF.map({
      timetableF =>
        val callingPointsF: Future[Option[LocationsCalledAtFromTimetable]] = timetableF.map((timetableOpt: Option[TimetableFound]) => {
          timetableOpt match {
            case Some(TimetableFound(tt, date, id, board, alight)) =>
              val stationsCalledAt: List[Location] = tt
                .locations
                .filter(_.pass.isEmpty)
                .flatMap(l => locationsService.findPriortiseOrrStations(l.tiploc))
                .filter(_.isOrrStation)

              val calledAtPoints: List[Location] = {
                val boardIndex = stationsCalledAt.map(_.id).indexOf(board)
                val alightIndex = stationsCalledAt.map(_.id).indexOf(alight) + 1
                stationsCalledAt.slice(boardIndex, alightIndex)
              }

              Some(LocationsCalledAtFromTimetable(tt, calledAtPoints, date, id, board, alight))

            case _ => None
          }
        })
        callingPointsF
    })
  }


   def getTimetablesFuture(groupedDataByRow: Map[String, Map[String, String]])(implicit executionContext: ExecutionContext): List[Future[Option[TimetableFound]]] = {
    groupedDataByRow.map(entry => {
      val (row, data) = entry
      val dateOpt = data.get(s"date_$row")
      val trainIdOpt = data.get(s"train-id_$row")
      val boardOpt = data.get(s"board_$row")
      val alightOpt = data.get(s"alight_$row")

      (dateOpt, trainIdOpt, boardOpt, alightOpt) match {
        case (Some(date), Some(trainId), Some(board), Some(alight)) =>
          val (y, m, d): (Int, Int, Int) = if (date.contains("-")) {
            val dateParts = date.split("-").map(_.toInt)
            (dateParts(0), dateParts(1), dateParts(2))
          } else (0, 0, 0)
          val timetableF: Future[Option[TimetableFound]] =
            timetableService
              .getTrain(trainId, y.toString, m.toString, d.toString)
              .map(_.map(TimetableFound(_, LocalDate.parse(date), trainId, board, alight)))
          timetableF
        case _ => Future.successful(None)
      }
    }).toList
  }

   def getTimetablesFutureFromTrainPlan(trainPlans: List[TrainPlanEntry])(implicit executionContext: ExecutionContext): List[Future[Option[TimetableFound]]] = {
    trainPlans.map(plan => {
      val trainId = plan.trainId
      val date = plan.boardDate

      val timetableF: Future[Option[TimetableFound]] =
        timetableService
          .getTrain(trainId, date.getYear.toString, date.getMonth.getValue.toString, date.getDayOfMonth.toString)
          .map(_.map(TimetableFound(_, date, trainId, plan.boardLocation.id, plan.alightLocation.id)))
      timetableF
    })
  }

}

object TrainPlanSorter {
  def apply(`this`: TrainPlanEntry, that: TrainPlanEntry): Boolean = {
    if(`this`.boardDate.isEqual(that.boardDate)) `this`.boardTime.isBefore(that.boardTime)
    else `this`.boardDate.isBefore(that.boardDate)
  }
}


object FormKeyTypes {
  type TypeOfKey = String
  type KeyName = String
  type KeyValue = String
}

case class TimetableFound(timetable: IndividualTimetable, date: LocalDate, trainId: String, board: String, alight: String)
case class LocationsCalledAtFromTimetable(timetable: IndividualTimetable, locations: List[Location], date: LocalDate, trainId: String, board: String, alight: String)