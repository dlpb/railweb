package controllers

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.route.{ListRoute, RoutesService}
import play.api.mvc._

import scala.collection.immutable.ListMap


@Singleton
class RouteController @Inject()(
                                       cc: ControllerComponents,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       routeService: RoutesService,
                                       jwtService: JWTService

) extends AbstractController(cc) {

  def showRouteDetailPage(from: String, to: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
   if(request.user.roles.contains(MapUser)){
     val route = routeService.getRoute(from, to)
     route match {
       case Some(r) =>
         val token = jwtService.createToken(request.user, new Date())
         Ok(views.html.route.route(
           request.user,
           r,
           routeService.getVisitsForRoute(r, request.user),
           token,
           routes.ApiAuthenticatedController.visitRouteWithParams(from, to),
           routes.ApiAuthenticatedController.removeLastVisitForRoute(from, to),
           routes.ApiAuthenticatedController.removeAllVisitsForRoute(from, to)
         )(request.request))
       case None => NotFound("Route combination not found.")
     }
   }
   else {
     Forbidden("User not authorized to view page")
   }
  }

  def showRouteListPage(nrRoutes: Boolean, srs: String, name: String, id: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
      def sortRoutes(a: ListRoute, b: ListRoute): Boolean = {
        if(a.srs.equals(b.srs))
          if(a.from.equals(b.from)) a.to.name < b.to.name
          else a.from.name < b.from.name
        else a.srs < b.srs
      }
      val routeList: List[ListRoute] = routeService.defaultListRoutes.toList.filter({
        r =>
          val nrFlag = if(nrRoutes) r.srs.contains(".") else true
          val srsFlag = if(!srs.equals("all")) r.srs.toLowerCase.contains(srs.toLowerCase) else true
          val nameFlag = if(!name.equals("all")) r.from.name.toLowerCase.contains(name.toLowerCase) || r.to.name.toLowerCase.contains(name.toLowerCase) else true
          val idFlag = if(!id.equals("all"))  r.from.id.toLowerCase.contains(id.toLowerCase) || r.to.id.toLowerCase.contains(id.toLowerCase) else true
          nrFlag && srsFlag && nameFlag && idFlag
      })
      val visited = routeService.getVisitedRoutes(request.user)

      def makeRouteKey(r: ListRoute) = {
        s"from:${r.from.id}-to:${r.to.id}"
      }

      val token = jwtService.createToken(request.user, new Date())
      val visits: Map[String, Boolean] = routeList.map({
        r =>
          val key = makeRouteKey(r)
          key -> visited.contains(key)
      }).toMap
      val formActions: Map[String, Call] = routeList.map({
        r =>
          val key = makeRouteKey(r)
          key -> routes.ApiAuthenticatedController.visitRouteFromList(r.from.id, r.to.id)
      }).toMap
      val routeMap: Map[String, ListRoute] = routeList.map({
        r =>
          val key = makeRouteKey(r)
          key -> r
      }).toMap

      val sortedRouteMap = ListMap(routeMap.toSeq.sortWith((a, b) => sortRoutes(a._2, b._2)) :_*)

      val visitedCount = visited.count({ v => routeMap.keySet.contains(v) })
      val availableCount = routeMap.size
      val percentage = (visitedCount.toDouble / availableCount.toDouble) * 100.0
      val formattedPercentage: String = f"$percentage%1.1f"

      Ok(views.html.route.index(
        request.user,
        sortedRouteMap,
        visits,
        formActions,
        token,
        visitedCount,
        availableCount,
        formattedPercentage,
        nrRoutes,
        id,
        name,
        srs))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
