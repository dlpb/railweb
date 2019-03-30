package controllers

import java.util.Date

import auth.api.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.{ListLocation, Location, LocationsService}
import play.api.mvc._


@Singleton
class LocationController @Inject()(
                                       cc: ControllerComponents,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       locationService: LocationsService,
                                       jwtService: JWTService

                                     ) extends AbstractController(cc) {
  private val logoutUrl = routes.AuthenticatedUserController.logout

  def showLocationDetailPage(id: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
   if(request.user.roles.contains(MapUser)){
     val location: Option[Location] = locationService.getLocation(id)
     val token = jwtService.createToken(request.user, new Date())
     location match {
       case Some(loc) => Ok(views.html.location(loc,
         locationService.getVisitsForLocation(loc, request.user),
         token,
         routes.ApiAuthenticatedController.visitLocationWithParams(id),
         routes.ApiAuthenticatedController.removeLastVisitForLocation(id),
         routes.ApiAuthenticatedController.removeAllVisitsForLocation(id)
       ))
       case None => NotFound("Location not found.")

     }
   }
   else {
     Forbidden("User not authorized to view page")
   }


  }

  def showLocationListPage() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
      val locations: Set[ListLocation] = locationService.defaultListLocations
      Ok(views.html.locations(locations))
    }
    else {
      Forbidden("User not authorized to view page")
    }


  }

}
