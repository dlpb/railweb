package controllers.api.locations.visit

import auth.api.AuthorizedAction
import javax.inject.{Inject, Singleton}
import models.auth.roles.VisitUser
import org.json4s.DefaultFormats
import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.location.LocationService
import services.visit.location.LocationVisitService

@Singleton
class VisitLocationsApiController @Inject()(
                                             env: Environment,
                                             cc: ControllerComponents,
                                             locationService: LocationService,
                                             locationVisitService: LocationVisitService,
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
              locationService.findFirstLocationByNameTiplocCrsOrId(loc) match {
                case Some(l) =>
                  locationVisitService.visitLocation(l, request.user)
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

          locationService.findFirstLocationByNameTiplocCrsOrId(id) match {
            case Some(l) =>
              locationVisitService.visitLocation(l, request.user)
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
          locationService.findFirstLocationByNameTiplocCrsOrId(id) match {
            case Some(l) =>
              locationVisitService.visitLocation(l, request.user)
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
          locationService.findFirstLocationByNameTiplocCrsOrId(id) match {
            case Some(l) =>
              locationVisitService.visitLocation(l, request.user)
              Redirect(controllers.location.list.tiploc.routes.LocationsByTiplocController.index())
            case None => NotFound
          }
        }
      }
    }


    def getAllVisitsForLocation(id: String) = {
      authAction { implicit request =>
        val location = locationService.findFirstLocationByNameTiplocCrsOrId(id)
        location match {
          case Some(loc) =>
            val visits: List[String] = locationVisitService.getVisitsForLocation(loc, request.user)
            Ok(Json.toJson(visits))
          case None => NotFound
        }
      }
    }

    def getAllVisitsForLocations() = {
      authAction { implicit request =>
        Ok(Json.toJson(locationVisitService.getVisitedLocations(request.user)))
      }
    }

    def removeLastVisitForLocation(id: String) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          locationService.findFirstLocationByNameTiplocCrsOrId(id) match {
            case Some(l) =>
              locationVisitService.deleteLastVisit(l, request.user)
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
          locationService.findFirstLocationByNameTiplocCrsOrId(id) match {
            case Some(l) =>
              locationVisitService.deleteAllVisits(l, request.user)
              Redirect(controllers.location.detail.routes.LocationDetailController.index(id))
            case None => NotFound
          }
        }
      }
    }
  }
