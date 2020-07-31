package controllers.api.route.visit

import auth.api.{AuthorizedAction, UserRequest}
import javax.inject.{Inject, Singleton}
import models.auth.roles.{PlanUser, VisitUser}
import models.location.{LocationsService, MapLocation}
import models.plan.timetable.TimetableService
import models.plan.trains.LocationTrainService
import models.route.{MapRoute, RoutesService}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

@Singleton
class VisitRouteApiController @Inject()(
                                            env: Environment,
                                            cc: ControllerComponents,
                                            locationService: LocationsService,
                                            routeService: RoutesService,
                                            trainService: LocationTrainService,
                                            timetableService: TimetableService,
                                            authAction: AuthorizedAction // NEW - add the action as a constructor argument
                                          )
  extends AbstractController(cc) {


    implicit val formats = DefaultFormats

    def visitRoute() = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          val from = request.request.body.asFormUrlEncoded.get("from").headOption
          val to = request.request.body.asFormUrlEncoded.get("to").headOption

          (from, to) match {
            case (Some(f), Some(t)) =>
              routeService.getRoute(f, t) match {
                case Some(r) => routeService.visitRoute(r, request.user)
                  Ok(s"Route found ${r.from.id}, ${r.to.id}, visiting")
                case None => NotFound
              }
            case _ => BadRequest
          }
        }
      }
    }

    def visitRouteWithParams(from: String, to: String) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          routeService.getRoute(from, to) match {
            case Some(r) => routeService.visitRoute(r, request.user)
              Redirect(controllers.route.detail.routes.RouteDetailController.index(from, to))
            case None => NotFound
          }
        }
      }
    }

    def visitRouteFromList(from: String, to: String) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          routeService.getRoute(from, to) match {
            case Some(r) => routeService.visitRoute(r, request.user)
              Redirect(controllers.route.routes.RouteController.index())
            case None => NotFound
          }
        }
      }
    }

    def getAllVisitsForRoutes() = {
      authAction { implicit request =>
        val visits = routeService.getVisitedRoutes(request.user)
        Ok(Json.toJson(visits))
      }
    }

    def getAllVisitsForRoute(from: String, to: String) = {
      authAction { implicit request =>
        val route = routeService.getRoute(from, to)
        route match {
          case Some(r) =>
            val visits: List[String] = routeService.getVisitsForRoute(r, request.user)
            Ok(Json.toJson(visits))
          case None => NotFound
        }
      }
    }

    def removeLastVisitForRoute(from: String, to: String) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          routeService.getRoute(from, to) match {
            case Some(r) =>
              routeService.deleteLastVisit(r, request.user)
              Redirect(controllers.route.detail.routes.RouteDetailController.index(from, to))
            case None => NotFound
          }
        }
      }
    }

    def removeAllVisitsForRoute(from: String, to: String) = {
      authAction { implicit request =>
        if (!request.user.roles.contains(VisitUser)) Unauthorized("User does not have the right role")
        else {
          routeService.getRoute(from, to) match {
            case Some(r) =>
              routeService.deleteAllVisits(r, request.user)
              Redirect(controllers.route.detail.routes.RouteDetailController.index(from, to))
            case None => NotFound
          }
        }
      }
    }

  }
