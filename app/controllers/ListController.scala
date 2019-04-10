package controllers


import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.list.ListService
import models.location.{Location, LocationsService, MapLocation}
import models.route.{MapRoute, Route, RoutesService}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

@Singleton
class ListController @Inject()(
                                 cc: ControllerComponents,
                                 authenticatedUserAction: AuthorizedWebAction,
                                 listService: ListService,
                                 locationsService: LocationsService,
                                 routesService: RoutesService,
                                 jwtService: JWTService

                               ) extends AbstractController(cc) {

  def showListPage(from: String, to: String, followFreightLinks: Boolean, followFixedLinks: Boolean) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {
      (locationsService.getLocation(from), locationsService.getLocation(to)) match {
        case (Some(f), Some(t)) =>
          val token = jwtService.createToken(request.user, new Date())
          val locations = listService.list(f, t, followFixedLinks, followFreightLinks)
          val mapRoutes: List[MapRoute] = getRoutes(locations)
          val mapLocations = locations map {l => MapLocation(l)}
          Ok(views.html.findRoute(request.user, token, mapLocations, mapRoutes, from, to, followFreightLinks, followFixedLinks))
        case _ => NotFound(views.html.findRoute(request.user, "", List(), List(), from, to, followFreightLinks, followFixedLinks))
      }
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def getRoutes(locations: List[Location]): List[MapRoute] = {
    def process(current: Location, rest: List[Location], accumulator: List[MapRoute]): List[MapRoute] = {
      rest match {
        case Nil => accumulator
        case head :: _ =>
          val route: List[MapRoute] = {
            val ft = routesService.getRoute(current.id, head.id)
            val tf = routesService.getRoute(head.id, current.id)
            if(ft.isEmpty)
              if(tf.isEmpty) List()
              else List(tf.get) map {MapRoute(_)}
            else List(ft.get) map {MapRoute(_)}
          }
          process(rest.head, rest.tail, route ++ accumulator)
      }
    }
    process(locations.head, locations.tail, List())
  }
}