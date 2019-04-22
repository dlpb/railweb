package controllers


import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.list.ListService
import models.location.{Location, LocationsService, MapLocation}
import models.route.{MapRoute, RoutesService}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

import scala.collection.immutable

@Singleton
class ListController @Inject()(
                                cc: ControllerComponents,
                                authenticatedUserAction: AuthorizedWebAction,
                                listService: ListService,
                                locationsService: LocationsService,
                                routesService: RoutesService,
                                jwtService: JWTService

                              ) extends AbstractController(cc) {

  def showListPage(waypoints: String, followFreightLinks: Boolean, followFixedLinks: Boolean) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {
      val locationsToRouteVia = waypoints.split("\n")

      var locations: List[Location] = List.empty
      var mapRoutes: List[MapRoute] = List.empty
      var mapLocations: List[MapLocation] = List.empty

      val token = jwtService.createToken(request.user, new Date())

      if (locationsToRouteVia.size >= 2) {
        for (i <- 0 until locationsToRouteVia.size - 1 ) {
          val from = locationsToRouteVia(i).trim.toUpperCase()
          val to = locationsToRouteVia(i + 1).trim.toUpperCase()
          (locationsService.getLocation(from.toUpperCase), locationsService.getLocation(to.toUpperCase)) match {
            case (Some(f), Some(t)) =>
              val routeLocations: List[Location] = listService.list(f, t, followFixedLinks, followFreightLinks)
              locations = locations ++ routeLocations
              mapRoutes = mapRoutes ++ getRoutes(routeLocations)
              mapLocations = mapLocations ++ (routeLocations map { l => MapLocation(l) })
            case _ =>
          }
        }
      }
      Ok(views.html.findRoute(request.user, token, mapLocations, mapRoutes, waypoints, followFreightLinks, followFixedLinks))

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
            if (ft.isEmpty)
              if (tf.isEmpty) List()
              else List(tf.get) map {
                MapRoute(_)
              }
            else List(ft.get) map {
              MapRoute(_)
            }
          }
          process(rest.head, rest.tail, route ++ accumulator)
      }
    }

    process(locations.head, locations.tail, List()).reverse
  }
}