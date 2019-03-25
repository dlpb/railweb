package controllers.api

import auth.api.{AuthorizedAction, UserRequest}
import javax.inject.{Inject, Singleton}
import models.location.LocationsService
import models.route.RoutesService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.io.Source

@Singleton
class ApiAuthenticatedController @Inject()(
                                            env: Environment,
                                            cc: ControllerComponents,
                                            authAction: AuthorizedAction // NEW - add the action as a constructor argument
                             )
  extends AbstractController(cc) {

  private val locations: LocationsService = makeLocationService()
  private val routes: RoutesService = makeRouteService()

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

  private def makeLocationService(): LocationsService ={
    val jsonString = Source.fromFile(System.getProperty("user.dir") + "/resources/data/static/locations.json").mkString
    LocationsService.makeLocationsService(jsonString)
  }

  private def makeRouteService(): RoutesService ={
    val jsonString = Source.fromFile(System.getProperty("user.dir") + "/resources/data/static/routes.json").mkString
    RoutesService.makeRoutesService(jsonString)
  }
}