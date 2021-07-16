package controllers.route

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.route.Route
import services.visit.route.RouteVisitService
import play.api.mvc._
import services.route.RouteService

import scala.collection.immutable.ListMap


@Singleton
class RouteController @Inject()(
                                 cc: ControllerComponents,
                                 authenticatedUserAction: AuthorizedWebAction,
                                 routeService: RouteService,
                                 routeVisitService: RouteVisitService,
                                 jwtService: JWTService

) extends AbstractController(cc) {

  def index(nrRoutes: Boolean, srs: String, name: String, id: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
      def sortRoutes(a: Route, b: Route): Boolean = {
        if(a.srsCode.equals(b.srsCode))
          if(a.from.equals(b.from)) a.to.id < b.to.id
          else a.from.id < b.from.id
        else a.srsCode < b.srsCode
      }
      val routeList: List[Route] = routeService.routes.toList.filter({
        r =>
          val nrFlag = if(nrRoutes) r.srsCode.contains(".") else true
          val srsFlag = if(!srs.equals("all")) r.srsCode.toLowerCase.contains(srs.toLowerCase) else true
          val nameFlag = if(!name.equals("all")) r.from.id.toLowerCase.contains(name.toLowerCase) || r.to.id.toLowerCase.contains(name.toLowerCase) else true
          val idFlag = if(!id.equals("all"))  r.from.id.toLowerCase.contains(id.toLowerCase) || r.to.id.toLowerCase.contains(id.toLowerCase) else true
          nrFlag && srsFlag && nameFlag && idFlag
      })
      val visited: List[String] = routeVisitService
        .getVisitedRoutes(request.user)
        .distinct
        .map(r => s"from:${r.from.id}-to:${r.to.id}")

      def makeRouteKeyFromRoute(r: Route) = {
        s"from:${r.from.id}-to:${r.to.id}"
      }

      val token = jwtService.createToken(request.user, new Date())
      val visits: Map[String, Boolean] = routeList.map({
        r =>
          val key = makeRouteKeyFromRoute(r)
          key -> visited.contains(key)
      }).toMap
      val formActions: Map[String, Call] = routeList.map({
        r =>
          val key = makeRouteKeyFromRoute(r)
          key -> controllers.api.route.visit.routes.VisitRouteApiController.visitRouteFromList(r.from.id, r.to.id)
      }).toMap
      val routeMap: Map[String, Route] = routeList.map({
        r =>
          val key = makeRouteKeyFromRoute(r)
          key -> r
      }).toMap

      val sortedRouteMap = ListMap(routeMap.toSeq.sortWith((a, b) => sortRoutes(a._2, b._2)) :_*)

      val visitedCount = visited.distinct.size
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
