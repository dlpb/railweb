package controllers.plan.highlight.callingpoints.upload

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding
import javax.inject.Inject
import models.auth.roles.PlanUser
import models.location.Location
import models.plan.highlight._
import models.plan.timetable.trains.TrainTimetableService
import models.srs.SrsService
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.location.LocationService

import scala.concurrent.Future

class TrainCallingPointUploadController @Inject()(
                                                   cc: ControllerComponents,
                                                   authenticatedUserAction: AuthorizedWebAction,
                                                   locationsService: LocationService,
                                                   timetableService: TrainTimetableService,
                                                   highlightTimetableService: HighlightTimetableService,
                                                   srsService: SrsService,
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
      val token = jwtService.createToken(request.user, new Date())

      try {
        val trainPlans: List[TrainPlanEntryParseResult] = data.get("trainPlan").map(text =>
        text
          .split(System.lineSeparator())
          .toList
        )
        .getOrElse(List.empty)
        .map(entry => TrainPlanEntryFromLine(entry)(locationsService, timetableService))


        val timetablesF = highlightTimetableService.getTimetablesFutureFromTrainPlan(trainPlans.flatMap(_.entry))
        val formDataToReturn = highlightTimetableService.makeReturnFormDataFromTrainPlans(trainPlans.flatMap(_.entry))

        generateResponse(request, token, data, formDataToReturn, timetablesF, trainPlans.flatMap(_.errors))
      }
      catch {
        case e: Exception =>
          BadRequest(views.html.plan.location.highlight.trains.newView(
            request.user,
            token,
            locationsService.locations.filter(_.isOrrStation).toList.sortBy(_.name),
            List.empty,
            List.empty,
            List.empty,
            List.empty,
            List.empty,
            srsService.getAll.map(srs => (srs.id, srs.name + " - " + srs.region)).toList.sortBy(_._1),
            "",
            0,
            0,
            0.0,
            List("Work In Progress - Plan - Highlight Locations", s"Error: ${e.getMessage}"),
            controllers.plan.highlight.callingpoints.routes.TrainCallingPointHighlightController.post())
          (request.request))
      }
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  private def generateResponse(
                                request: WebUserContext[AnyContent],
                                token: String,
                                data: Map[String, String],
                                formDataToReturn: List[Map[String, (String, String)]],
                                timetablesF: List[Future[Option[TimetableFound]]],
                                errors: List[Throwable]) = {
    val locationsCalledAtF: List[Future[Option[LocationsCalledAtFromTimetable]]] = highlightTimetableService.getLocationsCalledAtFuture(timetablesF)
    val mapLocationsCalledAt: List[Location] = highlightTimetableService.getMapLocationsForLocationsCalledAt(locationsCalledAtF)

    val trainDataPlanF: List[Future[Option[TrainPlanEntry]]] = highlightTimetableService.getTrainPlanEntriesFuture(locationsCalledAtF)
    val dataPlanEntriesF: Future[List[TrainPlanEntry]] = highlightTimetableService.getSortedTrainPlanEntries(trainDataPlanF)
    val trainDataPlan = highlightTimetableService.getTrainPlan(dataPlanEntriesF)

    val trainPlanEncoded = BaseEncoding.base64().encode(trainDataPlan.getBytes(Charsets.UTF_8))

    Ok(views.html.plan.location.highlight.trains.newView(
      request.user,
      token,
      locationsService.locations.filter(_.isOrrStation).toList.sortBy(_.name),
      formDataToReturn,
      mapLocationsCalledAt,
      List.empty,
      List.empty,
      List.empty,
      srsService.getAll.map(srs => (srs.id, srs.name + " - " + srs.region)).toList.sortBy(_._1),
      trainPlanEncoded,
      mapLocationsCalledAt.size,
      0,
      0.0,
      List("Work In Progress - Plan - Highlight Locations") ++ errors.map(_.getMessage),
      controllers.plan.highlight.callingpoints.routes.TrainCallingPointHighlightController.post())
    (request.request))
  }
}
