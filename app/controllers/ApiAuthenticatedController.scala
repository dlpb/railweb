package controllers

import auth.api.{AuthorizedAction, UserRequest}
import javax.inject.{Inject, Singleton}
import models.location.LocationsService
import models.route.RoutesService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, AnyContentAsFormUrlEncoded, ControllerComponents}

@Singleton
class ApiAuthenticatedController @Inject()(
                                            env: Environment,
                                            cc: ControllerComponents,
                                            locations: LocationsService,
                                            routes: RoutesService,
                                            authAction: AuthorizedAction // NEW - add the action as a constructor argument
                                          )
  extends AbstractController(cc) {


  implicit val formats = DefaultFormats

  def getLocationsForMap() = {
    authAction { implicit request: UserRequest[AnyContent] =>
      Ok(write(locations.mapLocations)).as(JSON)
    }
  }

  def getLocationsForList() = {
    authAction { implicit request =>
      Ok(write(locations.defaultListLocations)).as(JSON)
    }
  }

  def getLocation(id: String) = {
    authAction { implicit request =>
      val loc = locations.getLocation(id)
      loc match {
        case Some(location) => Ok(write(location)).as(JSON)
        case None => NotFound
      }
    }
  }

  def getRoutesForMap() = {
    authAction { implicit request =>
      Ok(write(routes.mapRoutes)).as(JSON)
    }
  }

  def getRoutesForList() = {
    authAction { implicit request =>
      Ok(write(routes.defaultListRoutes)).as(JSON)
    }
  }

  def getRoute(from: String, to: String) = {
    authAction { implicit request =>
      val route = routes.getRoute(from, to)
      route match {
        case Some(routing) => Ok(write(routing)).as(JSON)
        case None => NotFound
      }
    }
  }

  def visitLocation() = {
    authAction { implicit request =>
      val id = request.request.body.asFormUrlEncoded.get("location").headOption

      id match {
        case Some(loc) =>
          locations.getLocation(loc) match {
            case Some(l) =>
              locations.visitLocation(l, request.user)
              Ok(s"Loc found ${l.id}, visiting")
            case None => NotFound
          }
        case None => BadRequest
      }
    }
  }

  def getAllVisitsForLocation(id: String) = {
    authAction { implicit request =>
      val location = locations.getLocation(id)
      location match {
        case Some(loc) =>
          val visits: List[String] = locations.getVisitsForLocation(loc, request.user)
          Ok(Json.toJson(visits))
        case None => NotFound
      }
    }
  }

  def removeLastVisitForLocation() = {
    authAction { implicit request =>
      val id = request.request.body.asFormUrlEncoded.get("location").headOption

      id match {
        case Some(loc) =>
          locations.getLocation(loc) match {
            case Some(l) =>
              locations.deleteLastVisit(l, request.user)
              Ok(s"Loc found ${l.id}, deleting last visit")
            case None => NotFound
          }
        case None => BadRequest
      }
    }
  }

  def removeAllVisitsForLocation() = {
    authAction { implicit request =>
      val id = request.request.body.asFormUrlEncoded.get("location").headOption

      id match {
        case Some(loc) =>
          locations.getLocation(loc) match {
            case Some(l) =>
              locations.deleteAllVisits(l, request.user)
              Ok(s"Loc found ${l.id}, deleting all visit")
            case None => NotFound
          }
        case None => BadRequest
      }
    }
  }

  def visitRoute() = {
    authAction { implicit request =>
      val from = request.request.body.asFormUrlEncoded.get("from").headOption
      val to = request.request.body.asFormUrlEncoded.get("to").headOption

      (from, to) match {
        case (Some(f), Some(t)) =>
          routes.getRoute(f,t) match {
            case Some(r) => routes.visitRoute(r, request.user)
              Ok(s"Route found ${r.from.id}, ${r.to.id}, visiting")
            case None => NotFound
          }
        case _ => BadRequest
      }
    }
  }

  def getAllVisitsForRoute(from: String, to: String) = {
    authAction { implicit request =>
      val route = routes.getRoute(from, to)
      route match {
        case Some(r) =>
          val visits: List[String] = routes.getVisitsForRoute(r, request.user)
          Ok(Json.toJson(visits))
        case None => NotFound
      }
    }
  }

  def removeLastVisitForRoute() = {
    authAction { implicit request =>
      val from = request.request.body.asFormUrlEncoded.get("from").headOption
      val to = request.request.body.asFormUrlEncoded.get("to").headOption

      (from, to) match {
        case (Some(f), Some(t)) =>
          routes.getRoute(f,t) match {
            case Some(r) =>
              routes.deleteLastVisit(r, request.user)
              Ok(s"Loc found ${r.from.id}, ${r.to.id}, deleting last visit")
            case None => NotFound
          }
        case _ => BadRequest
      }
    }
  }

  def removeAllVisitsForRoute() = {
    authAction { implicit request =>
      val from = request.request.body.asFormUrlEncoded.get("from").headOption
      val to = request.request.body.asFormUrlEncoded.get("to").headOption

      (from, to) match {
        case (Some(f), Some(t)) =>
          routes.getRoute(f,t) match {
            case Some(r) =>
              routes.deleteAllVisits(r, request.user)
              Ok(s"Loc found ${r.from.id}, ${r.to.id}, deleting all visit")
            case None => NotFound
          }
        case _ => BadRequest
      }
    }
  }
}