package controllers.api.locations.visit

import auth.api.{AuthorizedAction, UserRequest}
import javax.inject.{Inject, Singleton}
import models.auth.roles.{PlanUser, VisitUser}
import models.location.{LocationsService, MapLocation}
import models.plan.timetable.location.LocationTimetableService
import models.plan.timetable.trains.TrainTimetableService
import models.route.RoutesService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

@Singleton
class VisitLocationsApiController @Inject()(
                                             env: Environment,
                                             cc: ControllerComponents,
                                             locationService: LocationsService,
                                             routeService: RoutesService,
                                             trainService: LocationTimetableService,
                                             timetableService: TrainTimetableService,
                                             authAction: AuthorizedAction // NEW - add the action as a constructor argument
                                          )
  extends AbstractController(cc) {


    implicit val formats = DefaultFormats

    def visitLocation() = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          val id = request.request.body.asFormUrlEncoded.get("location").headOption

          id match {
            case Some(loc) =>
              locationService.getLocation(loc) match {
                case Some(l) =>
                  locationService.visitLocation(l, request.user)
                  Ok(s"Loc found ${l.id}, visiting")
                case None => NotFound
              }
            case None => BadRequest
          }
        }
      }
    }

    def visitLocationWithParams(id: String) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {

          locationService.getLocation(id) match {
            case Some(l) =>
              locationService.visitLocation(l, request.user)
              Redirect(controllers.location.detail.routes.LocationDetailController.index(id))
            case None => NotFound
          }
        }
      }
    }

    def visitLocationFromCrsList(id: String) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          locationService.getLocation(id) match {
            case Some(l) =>
              locationService.visitLocation(l, request.user)
              Redirect(controllers.location.list.crs.routes.LocationsByCrsController.index())
            case None => NotFound
          }
        }
      }
    }

    def visitLocationFromTiplocList(id: String) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          locationService.getLocation(id) match {
            case Some(l) =>
              locationService.visitLocation(l, request.user)
              Redirect(controllers.location.list.tiploc.routes.LocationsByTiplocController.index())
            case None => NotFound
          }
        }
      }
    }


    def getAllVisitsForLocation(id: String) = {
      authAction { implicit request =>
        val location = locationService.getLocation(id)
        location match {
          case Some(loc) =>
            val visits: List[String] = locationService.getVisitsForLocation(loc, request.user)
            Ok(Json.toJson(visits))
          case None => NotFound
        }
      }
    }

    def getAllVisitsForLocations() = {
      authAction { implicit request =>
        Ok(Json.toJson(locationService.getVisitedLocations(request.user)))
      }
    }

    def removeLastVisitForLocation(id: String) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          locationService.getLocation(id) match {
            case Some(l) =>
              locationService.deleteLastVisit(l, request.user)
              Redirect(controllers.location.detail.routes.LocationDetailController.index(id))
            case None => NotFound
          }
        }
      }
    }

    def removeAllVisitsForLocation(id: String) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          locationService.getLocation(id) match {
            case Some(l) =>
              locationService.deleteAllVisits(l, request.user)
              Redirect(controllers.location.detail.routes.LocationDetailController.index(id))
            case None => NotFound
          }
        }
      }
    }
  }
