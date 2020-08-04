package controllers.plan.highlight.callingpoints

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.list.PathService
import models.location.{Location, LocationsService, MapLocation}
import models.plan.timetable.TimetableDateTimeHelper
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.model.train.IndividualTimetable
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.collection.{immutable, mutable}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class TrainCallingPointHighlightController @Inject()(
                                                      cc: ControllerComponents,
                                                      authenticatedUserAction: AuthorizedWebAction,
                                                      locationsService: LocationsService,
                                                      pathService: PathService,
                                                      trainService: LocationTimetableService,
                                                      timetableService: TrainTimetableService,
                                                      jwtService: JWTService

                              ) extends AbstractController(cc) with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def newView() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      Ok(views.html.plan.location.highlight.trains.newView(
        request.user,
        token,
        locationsService.getLocations.filter(_.isOrrStation).sortBy(_.name),
        List.empty,
        List.empty,
        List.empty,
        List.empty,
        "",
        List("Work In Progress - Plan - Highlight Locations"),
        controllers.plan.highlight.callingpoints.routes.TrainCallingPointHighlightController.post())
      (request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  case class DummyForm(id: String)
  val form = Form(mapping("id" -> text)(DummyForm.apply)(DummyForm.unapply))

  def post( )= authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      val dynamicForm = form.bindFromRequest()
      val data: Map[String, String] = dynamicForm.data.view.filterKeys(_.contains("-")).toMap

      println("Processing called at points")
      println(data)

      val groupedDataByRow: Map[String, Map[String, String]] = data
        .keys
        .groupBy(key => key.split("_").toList.last)
        .map(row => {
          row._1 -> row._2.map(key => key -> data(key)).toMap
        })

      val formDataToReturn: List[Map[String, (String, String)]] = groupedDataByRow.map(entry => {
        val (row, data) = entry
        val locationKey = s"location_$row"
        val dateKey = s"date_$row"
        val trainKey = s"train-id_$row"
        val boardKey = s"board_$row"
        val alightKey = s"alight_$row"
        val date = data.getOrElse(dateKey, "")
        val trainId = data.getOrElse(trainKey, "")
        val board = data.getOrElse(boardKey, "")
        val alight = data.getOrElse(alightKey, "")
        val location = data.getOrElse(locationKey, "")
        val map = Map("date" -> (dateKey, date), "trainId" -> (trainKey, trainId), "board" -> (boardKey, board), "alight" -> (alightKey, alight), "location" -> (locationKey, location))
        map
      }).toList

      case class TimetableFound(timetable: IndividualTimetable, date: String, trainId: String, board: String, alight: String)
      case class LocationsCalledAtFromTimetable(timetable: IndividualTimetable, locations: List[Location], date: String, trainId: String, board: String, alight: String)

      val timetablesF: List[Future[Option[TimetableFound]]] = groupedDataByRow.map(entry => {
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
            val timetableF: Future[Option[TimetableFound]] = timetableService.getTrain(trainId, y.toString, m.toString, d.toString ).map(_.map(TimetableFound(_, date, trainId, board, alight)))
            timetableF
          case _ => Future.successful(None)
        }
      }).toList

      val locationsCalledAtF: List[Future[Option[LocationsCalledAtFromTimetable]]] = timetablesF.map({
        timetableF =>
          val callingPointsF: Future[Option[LocationsCalledAtFromTimetable]] = timetableF.map((timetableOpt: Option[TimetableFound]) => {
            timetableOpt match {
              case Some(TimetableFound(tt, date, id, board, alight)) =>
                val stationsCalledAt: List[Location] = tt
                  .locations
                  .filter(_.pass.isEmpty)
                  .flatMap(l => locationsService.findLocation(l.tiploc))
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

      val mapLocationsCalledAtF: Future[List[MapLocation]] = Future.sequence(locationsCalledAtF)
        .map(_.flatten)
        .map(_.flatMap(_.locations.map(MapLocation(_))))

      val mapLocationsCalledAt = Await.result(mapLocationsCalledAtF, Duration(30, "seconds"))

      val trainDataPlanF: List[Future[Option[String]]] = locationsCalledAtF.map(f =>
        f.map(o =>
          o.map(l => {
            val date = l.date
            val timetable = l.timetable
            val boardTimetableEntry = timetable.locations.find(_.tiploc.equals(l.board))
            val alightTimetableEntry = timetable.locations.find(_.tiploc.equals(l.alight))
            val boardTime = boardTimetableEntry.map(l => l.publicDeparture.getOrElse(l.departure.getOrElse("----"))).getOrElse("")
            val boardPlatform = boardTimetableEntry.map(_.platform).getOrElse("")
            val alightTime = alightTimetableEntry.map(l => l.publicArrival.getOrElse(l.arrival.getOrElse("----"))).getOrElse("")
            val alightPlatform = alightTimetableEntry.map(_.platform).getOrElse("")
            val boardCrs = boardTimetableEntry.flatMap(l => locationsService.findLocation(l.tiploc).map(_.crs.headOption.getOrElse(l.tiploc))).getOrElse("")
            val alightCrs = alightTimetableEntry.flatMap(l => locationsService.findLocation(l.tiploc).map(_.crs.headOption.getOrElse(l.tiploc))).getOrElse("")
            val calledAtCrs = l.locations.flatMap(_.crs.headOption).mkString(",")

            val boardTimeFormatString = f"$boardTime%4s"
            val alightTimeFormatString = f"$alightTime%4s"
            val boardCrsFormatString = f"$boardCrs%3s"
            val alightCrsFormatString = f"$alightCrs%3s"
            val boardPlatformFormatString = f"$boardPlatform%4s"
            val alightPlatformFormatString = f"$alightPlatform%4s"

            val row = s"$date $boardTimeFormatString $boardCrsFormatString $boardPlatformFormatString $alightPlatformFormatString $alightCrsFormatString $alightTimeFormatString https://www.realtimetrains.co.uk/train/${l.trainId}/$date/detailed $calledAtCrs"

            row
          })
        )
      )

      val dataPlanEntriesF = Future.sequence(trainDataPlanF).map(_.flatten)
      val trainDataPlan = Await.result(dataPlanEntriesF, Duration(30, "seconds")).mkString("\n")

      println(data)
      println(mapLocationsCalledAt)
      println(trainDataPlan)

      val trainPlanEncoded = BaseEncoding.base64().encode(trainDataPlan.getBytes(Charsets.UTF_8))

      Ok(views.html.plan.location.highlight.trains.newView(
        request.user,
        token,
        locationsService.getLocations.filter(_.isOrrStation).sortBy(_.name),
        formDataToReturn,
        mapLocationsCalledAt,
        List.empty,
        List.empty,
        trainPlanEncoded,
        List("Work In Progress - Plan - Highlight Locations"),
        controllers.plan.highlight.callingpoints.routes.TrainCallingPointHighlightController.post())
      (request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def index(trainsAndStations: String, srsLocations: String, locations: String, year: Int, month: Int, day: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())
      val mapLocations: List[MapLocation] =
        if(locations.isEmpty) { List.empty[MapLocation] }
        else locations
          .replaceAll("\\s+", ",")
          .split(",")
          .flatMap {
            locationsService.findLocation
          }
          .map {
            MapLocation(_)
          }.toList

      val srs = srsLocations
        .replaceAll("\\s+", ",")
        .split(",")

      val srsMapLocations: List[MapLocation] = if(srsLocations.isEmpty) List.empty[MapLocation] else locationsService
        .getLocations
        .filter {
          loc =>
            srs.contains(loc.nrInfo.map {
              _.srs
            }.getOrElse(" ")) ||
              srs.contains(loc.nrInfo.map {
                _.srs + " "
              }.getOrElse(" ").substring(0, 1))
        }
        .map {
          MapLocation(_)
        }

      val allStations = (srsMapLocations.toSet ++ mapLocations.toSet).toList

      val trainIdStationList: List[(String, String, String)] = trainsAndStations
        .linesWithSeparators
        .map {
          line =>
            val parts = line.split(",")
            (parts(0), parts(1), parts(2))
        }.toList

      val trainsF: List[Future[List[MapLocation]]] = trainIdStationList map {
        tsl =>
          val (train, from, to) = tsl
          Future {
            val tt = timetableService.getTrain(train, year.toString, month.toString, day.toString)
            val result: Future[List[MapLocation]] = tt.map {
              _.map {
                t: IndividualTimetable =>
                  val locs = t
                    .locations
                    .filter(l => l.publicArrival.isDefined || l.publicDeparture.isDefined)
                    .map {
                      _.tiploc
                    }.flatMap {
                      locationsService.findLocation
                    }
                  val fromIndexMaybe = locs.map(_.id).indexOf(from.trim)
                  val toIndexMaybe = locs.map(_.id).indexOf(to.trim)

                  val fromIndex = if(fromIndexMaybe < 0) 0 else fromIndexMaybe
                  val toIndex = if(toIndexMaybe < 0) locs.size - 1
                  else if (toIndexMaybe < locs.size) toIndexMaybe + 1
                  else toIndexMaybe


                  val sliced = locs
                    .slice(fromIndex, toIndex)
                    .map {
                      l => MapLocation(l)
                    }
                  sliced


              }.getOrElse(List.empty)
            }
            result

          }.flatten
      }
      val calledAt: List[MapLocation] = Await.result(Future.sequence(trainsF), Duration(30, "second")).flatten.distinct
      val notCalledAt = allStations.diff(calledAt)
      val percentage = if(allStations.nonEmpty) calledAt.size*1.0d / allStations.size*1.0d else 100.0d
      Ok(views.html.plan.location.highlight.trains.index(request.user, token, calledAt, notCalledAt, allStations, percentage, trainsF.size, trainsAndStations, srsLocations, locations, List("Work In Progress - Plan - Highlight Locations"), year, month, day)(request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

}

