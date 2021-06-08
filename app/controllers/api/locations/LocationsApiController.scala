package controllers.api.locations

import auth.api.AuthorizedAction
import javax.inject.{Inject, Singleton}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write
import play.api.Environment
import play.api.mvc.{AbstractController, ControllerComponents}
import services.location.LocationService

@Singleton
class LocationsApiController @Inject()(
                                        env: Environment,
                                        cc: ControllerComponents,
                                        locationService: LocationService,
                                        authAction: AuthorizedAction // NEW - add the action as a constructor argument
                                          )
  extends AbstractController(cc) {


    implicit val formats = DefaultFormats

    def getLocationsForList() = {
      authAction { implicit request =>
        Ok(write(locationService.sortedListLocationsGroupedByTiploc)).as(JSON)
      }
    }

    def getLocation(id: String) = {
      authAction { implicit request =>
        val loc = locationService.findFirstLocationByNameTiplocCrsOrId(id)
        loc match {
          case Some(location) => Ok(write(location)).as(JSON)
          case None => NotFound
        }
      }
    }



  }
