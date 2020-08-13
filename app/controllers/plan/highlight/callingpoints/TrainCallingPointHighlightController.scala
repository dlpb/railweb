package controllers.plan.highlight.callingpoints

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.location.{LocationsService, MapLocation}
import models.plan.highlight.{HighlightTimetableService, LocationsCalledAtFromTimetable, TimetableFound, TrainPlanEntry}
import models.plan.route.pointtopoint.PathService
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.model.train.IndividualTimetable
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class TrainCallingPointHighlightController @Inject()(
                                                      cc: ControllerComponents,
                                                      authenticatedUserAction: AuthorizedWebAction,
                                                      locationsService: LocationsService,
                                                      pathService: PathService,
                                                      trainService: LocationTimetableService,
                                                      timetableService: TrainTimetableService,
                                                      highlightTimetableService: HighlightTimetableService,
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

      val groupedDataByRow: Map[String, Map[String, String]] = highlightTimetableService.getTrainDataFromFormEntryGroupedByRow(data)

      val formDataToReturn: List[Map[String, (String, String)]] = highlightTimetableService.makeReturnDataFromForm(groupedDataByRow)

      val timetablesF: List[Future[Option[TimetableFound]]] = highlightTimetableService.getTimetablesFuture(groupedDataByRow)

      generateResponse(request, token, data, formDataToReturn, timetablesF)
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }


  private def generateResponse(request: WebUserContext[AnyContent], token: String, data: Map[String, String], formDataToReturn: List[Map[String, (String, String)]], timetablesF: List[Future[Option[TimetableFound]]]) = {
    val locationsCalledAtF: List[Future[Option[LocationsCalledAtFromTimetable]]] = highlightTimetableService.getLocationsCalledAtFuture(timetablesF)
    val mapLocationsCalledAt: List[MapLocation] = highlightTimetableService.getMapLocationsForLocationsCalledAt(locationsCalledAtF)

    val trainDataPlanF: List[Future[Option[TrainPlanEntry]]] = highlightTimetableService.getTrainPlanEntriesFuture(locationsCalledAtF)
    val dataPlanEntriesF: Future[List[TrainPlanEntry]] = highlightTimetableService.getSortedTrainPlanEntries(trainDataPlanF)
    val trainDataPlan = highlightTimetableService.getTrainPlan(dataPlanEntriesF)

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

  def index(trainsAndStations: String, srsLocations: String, locations: String, year: Int, month: Int, day: Int) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())
      val mapLocations: List[MapLocation] =
        if(locations.isEmpty) { List.empty[MapLocation] }
        else locations
          .replaceAll("\\s+", ",")
          .split(",")
          .flatMap {
            locationsService.findPriortiseOrrStations
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
                      locationsService.findPriortiseOrrStations
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
