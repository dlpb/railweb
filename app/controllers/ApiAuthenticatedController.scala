package controllers

import auth.api.{AuthorizedAction, UserRequest}
import javax.inject.{Inject, Singleton}
import models.location.LocationsService
import models.route.RoutesService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
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
            case Some(loc) =>
              Ok(s"Loc found $loc")
            case None => NotFound
          }
        case None => BadRequest
      }
    }
  }
}