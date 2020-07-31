package controllers.api.authenticated

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
import play.api.mvc.{AbstractController, AnyContent, Call, ControllerComponents}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, TimeoutException}

@Singleton
class ApiAuthenticatedController @Inject()(
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

  def getLocationsForMap() = {
    authAction { implicit request: UserRequest[AnyContent] =>
      Ok(write(locationService.mapLocations)).as(JSON)
    }
  }

  def getLocationsForList() = {
    authAction { implicit request =>
      Ok(write(locationService.defaultListLocations)).as(JSON)
    }
  }

  def getLocation(id: String) = {
    authAction { implicit request =>
      val loc = locationService.getLocation(id)
      loc match {
        case Some(location) => Ok(write(location)).as(JSON)
        case None => NotFound
      }
    }
  }

  def getRoutesForMap() = {
    authAction { implicit request =>
      Ok(write(routeService.mapRoutes)).as(JSON)
    }
  }

  def getRoutesForList() = {
    authAction { implicit request =>
      Ok(write(routeService.defaultListRoutes)).as(JSON)
    }
  }

  def getRoute(from: String, to: String) = {
    authAction { implicit request =>
      val route = routeService.getRoute(from, to)
      route match {
        case Some(routing) => Ok(write(routing)).as(JSON)
        case None => NotFound
      }
    }
  }

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

  def getSimpleMapLocationsForTrain(train: String, year: Int, month: Int, day: Int) = {
    authAction { implicit request =>
      if (!request.user.roles.contains(PlanUser)) Unauthorized("User does not have the right role")
      else {
        val eventualResult: Future[Option[List[MapLocation]]] = timetableService.getTrain(train, year.toString, month.toString, day.toString).map {
          f =>
            f map {
              t =>
                timetableService.createSimpleMapLocations(t)
            }
        }(scala.concurrent.ExecutionContext.Implicits.global)
        try {
          val mapDetails = Await.result(eventualResult, Duration(30, "second")).getOrElse(List())

          Ok(write(mapDetails))
        }
        catch{
          case e: TimeoutException =>
            InternalServerError(s"Could not get Map Simple Map Details for $train")
        }
      }
    }
  }

  def getDetailedMapLocationsForTrain(train: String, year: Int, month: Int, day: Int) = {
    authAction { implicit request =>
      if (!request.user.roles.contains(PlanUser)) Unauthorized("User does not have the right role")
      else {
        val eventualResult: Future[Option[List[MapLocation]]] = timetableService.getTrain(train,  year.toString, month.toString, day.toString).map {
          f =>
            f map {
              t =>
                timetableService.createDetailedMapLocations(t)
            }
        }(scala.concurrent.ExecutionContext.Implicits.global)
        try {
          val mapDetails = Await.result(eventualResult, Duration(30, "second")).getOrElse(List())

          Ok(write(mapDetails))
        }
        catch{
          case e: TimeoutException =>
            InternalServerError(s"Could not get Map Detailed Map Details for $train")
        }
      }
    }
  }

  def createMapRouteForTrain(train: String, year: Int, month: Int, day: Int) = {
    authAction { implicit request =>
      if (!request.user.roles.contains(PlanUser)) Unauthorized("User does not have the right role")
      else {
        val eventualResult: Future[Option[List[MapRoute]]] = timetableService.getTrain(train,  year.toString, month.toString, day.toString).map {
          f =>
            f map {
              t =>
                timetableService.createSimpleMapRoutes(t)
            }
        }(scala.concurrent.ExecutionContext.Implicits.global)
        try {
          val mapDetails = Await.result(eventualResult, Duration(30, "second")).getOrElse(List())

          Ok(write(mapDetails))
        }
        catch{
          case e: TimeoutException =>
            println("timed out")
            InternalServerError(s"Could not get Map Detailed Map Details for $train")
        }
      }
    }
  }


}