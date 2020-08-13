package controllers.plan.highlight.callingpoints.upload

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime}
import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.location.{Location, LocationsService, MapLocation}
import models.plan.highlight.{HighlightTimetableService, LocationsCalledAtFromTimetable, TimetableFound, TrainPlanEntry, TrainPlanEntryFromLine}
import models.plan.route.pointtopoint.PathService
import models.plan.timetable.TimetableDateTimeHelper
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.timetable.model.train.IndividualTimetable
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source

class TrainCallingPointUploadController @Inject()(
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

  def index() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(PlanUser)) {
      val token = jwtService.createToken(request.user, new Date())

      Ok(views.html.plan.location.highlight.trains.upload.index(
        request.user,
        token,
        List("Work In Progress - Plan - Highlight Locations"),
        controllers.plan.highlight.callingpoints.upload.routes.TrainCallingPointUploadController.post())
      (request.request))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  case class DummyForm(id: String)

  val form = Form(mapping("id" -> text)(DummyForm.apply)(DummyForm.unapply))

  def post(): Action[AnyContent] = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>

    if (request.user.roles.contains(PlanUser)) {

      val dynamicForm = form.bindFromRequest()
      val data: Map[String, String] = dynamicForm.data.view.filterKeys(_.contains("trainPlan")).toMap

      val trainPlans: List[TrainPlanEntry] = data.get("trainPlan").map(text =>
        text
          .split(System.lineSeparator())
          .toList
      )
        .getOrElse(List.empty)
        .flatMap(entry => TrainPlanEntryFromLine(entry)(locationsService))


      println(trainPlans)

      val token = jwtService.createToken(request.user, new Date())
      try {

        val timetablesF = highlightTimetableService.getTimetablesFutureFromTrainPlan(trainPlans)
        val formDataToReturn = highlightTimetableService.makeReturnFormDataFromTrainPlans(trainPlans)

          generateResponse(request, token, data, formDataToReturn, timetablesF)
      }
      catch {
        case e: Exception =>
          BadRequest(views.html.plan.location.highlight.trains.newView(
            request.user,
            token,
            locationsService.getLocations.filter(_.isOrrStation).sortBy(_.name),
            List.empty,
            List.empty,
            List.empty,
            List.empty,
            "",
            List("Work In Progress - Plan - Highlight Locations", s"Something went wrong processing the train plan: ${e.getMessage}"),
            controllers.plan.highlight.callingpoints.routes.TrainCallingPointHighlightController.post())
          (request.request))
      }
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

    println("Train Plan Result")
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
}
