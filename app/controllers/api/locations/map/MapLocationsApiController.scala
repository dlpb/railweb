package controllers.api.locations.map

import auth.api.{AuthorizedAction, UserRequest}
import javax.inject.{Inject, Singleton}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.location.LocationService

@Singleton
class MapLocationsApiController @Inject()(
                                           env: Environment,
                                           cc: ControllerComponents,
                                           locationService: LocationService,
                                           authAction: AuthorizedAction // NEW - add the action as a constructor argument
                                          )
  extends AbstractController(cc) {

    implicit val formats = DefaultFormats

    def getLocationsForMap() = {
      authAction { implicit request: UserRequest[AnyContent] =>
        Ok(write(locationService.mapLocations)).as(JSON)
      }
    }
  }
