package controllers.location.list.tiploc

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.ListLocation
import play.api.mvc._
import services.location.LocationService
import services.visit.location.LocationVisitService


@Singleton
class LocationsByTiplocController @Inject()(
                                             cc: ControllerComponents,
                                             authenticatedUserAction: AuthorizedWebAction,
                                             locationService: LocationService,
                                             locationVisitService: LocationVisitService,
                                             jwtService: JWTService

                                           ) extends AbstractController(cc) {


  def index(orr: Boolean, operator: String, name: String, id: String, srs: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if (request.user.roles.contains(MapUser)) {
      val token = jwtService.createToken(request.user, new Date())
      val locations: List[ListLocation] = locationService.sortedListLocationsGroupedByTiploc
        .filter({
          loc =>
            val orrFlag = if (orr) loc.orrStation else true
            val operatorFlag = if (!operator.equals("all")) loc.operator.toLowerCase.contains(operator.toLowerCase) else true
            val nameFlag = if (!name.equals("all")) loc.name.toLowerCase.contains(name.toLowerCase) else true
            val idFlag = if (!id.equals("all")) loc.id.toLowerCase.contains(id.toLowerCase) else true
            val srsFlag = if (!srs.equals("all")) loc.srs.toLowerCase.contains(srs.toLowerCase) else true
            orrFlag && operatorFlag && nameFlag && idFlag && srsFlag
        })
      val visited = locationVisitService.getVisitedLocations(request.user)

      val visits: Map[String, Boolean] = locations.map({
        l =>
          l.id -> visited.contains(l.id)
      }).toMap

      val formActions: Map[String, Call] = locations.map({
        loc =>
          loc.id -> controllers.api.locations.visit.routes.VisitLocationsApiController.visitLocationFromTiplocList(loc.id)
      }).toMap
      val locIds = locations.map {
        _.id
      }

      val visitedLocs = visited.count({ v => locIds.contains(v) })
      val availableLocs = locations.size
      val percentage = (visitedLocs.toDouble / availableLocs.toDouble) * 100.0
      val formattedPercentage: String = f"$percentage%1.1f"
      Ok(views.html.locations.list.tiploc.index(
        request.user,
        locations,
        visits,
        formActions,
        token,
        visitedLocs,
        availableLocs,
        formattedPercentage,
        orr,
        name,
        id,
        operator,
        srs
      ))
    }
    else {
      Forbidden("User not authorized to view page")
    }
  }
}
