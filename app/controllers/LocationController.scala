package controllers

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.{GroupedListLocation, ListLocation, Location, LocationsService}
import play.api.mvc._


@Singleton
class LocationController @Inject()(
                                       cc: ControllerComponents,
                                       authenticatedUserAction: AuthorizedWebAction,
                                       locationService: LocationsService,
                                       jwtService: JWTService

                                     ) extends AbstractController(cc) {

  def showLocationDetailPage(id: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
   if(request.user.roles.contains(MapUser)){
     val location: Option[Location] = locationService.getLocation(id)
     val token = jwtService.createToken(request.user, new Date())
     location match {
       case Some(loc) => Ok(views.html.locations.location(
         request.user,
         loc,
         locationService.getVisitsForLocation(loc, request.user),
         token,
         routes.ApiAuthenticatedController.visitLocationWithParams(id),
         routes.ApiAuthenticatedController.removeLastVisitForLocation(id),
         routes.ApiAuthenticatedController.removeAllVisitsForLocation(id)
       )(request.request))
       case None => NotFound("Location not found.")
     }
   }
   else {
     Forbidden("User not authorized to view page")
   }
  }

  def showLocationHomePage() = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
     Ok(views.html.locations.index(
       request.user,
        )(request.request))
      }
    else {
      Forbidden("User not authorized to view page")
    }
  }

  def showLocationListPageByCrs(orr: Boolean, operator: String, name: String, id: String, srs: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
      val token = jwtService.createToken(request.user, new Date())
      val locationTiplocs = locationService.defaultListLocations.map(_.id)
      val locations: List[GroupedListLocation] = locationService.groupedByCrsListLocations
        .filter({
          loc =>
            val orrFlag = if(orr) loc.orrStation else true
            val operatorFlag = if(!operator.equals("all")) loc.operator.toLowerCase.contains(operator.toLowerCase) else true
            val nameFlag = if(!name.equals("all")) loc.name.toLowerCase.contains(name.toLowerCase) else true
            val idFlag = if(!id.equals("all")) loc.id.toLowerCase.contains(id.toLowerCase) else true
            val srsFlag = if(!srs.equals("all")) loc.srs.toLowerCase.contains(srs.toLowerCase) else true
            orrFlag && operatorFlag && nameFlag && idFlag && srsFlag
        })

      val visited = locationService.getVisitedLocations(request.user)
      val groupedVisited: List[String] = locationService.getVisitedLocationsByCrs(request.user)

      val groupVisits: Map[String, Boolean] = locations.map {
        l =>
          l.id -> groupedVisited.contains(l.id)
      }.toMap


      val visits: Map[String, Boolean] = locationTiplocs.map({
        l =>
          l -> visited.contains(l)
      }).toMap

      val formActions:Map[String, Call] = locationTiplocs.map({
        loc =>
          loc -> routes.ApiAuthenticatedController.visitLocationFromList(loc)
      }).toMap
      val locIds = locations.map{_.id}

      val visitedLocs = groupedVisited.count({ v => locIds.contains(v) })
      val availableLocs = locations.size
      val percentage = (visitedLocs.toDouble / availableLocs.toDouble) * 100.0
      val formattedPercentage: String = f"$percentage%1.1f"
      Ok(views.html.locations.groupedByCrs(
        request.user,
        locations,
        visits,
        groupVisits,
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

  def showLocationListPageByTiploc(orr: Boolean, operator: String, name: String, id: String, srs: String) = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    if(request.user.roles.contains(MapUser)){
      val token = jwtService.createToken(request.user, new Date())
      val locations: List[ListLocation] = locationService.defaultListLocations
        .filter({
          loc =>
            val orrFlag = if(orr) loc.orrStation else true
            val operatorFlag = if(!operator.equals("all")) loc.operator.toLowerCase.contains(operator.toLowerCase) else true
            val nameFlag = if(!name.equals("all")) loc.name.toLowerCase.contains(name.toLowerCase) else true
            val idFlag = if(!id.equals("all")) loc.id.toLowerCase.contains(id.toLowerCase) else true
            val srsFlag = if(!srs.equals("all")) loc.srs.toLowerCase.contains(srs.toLowerCase) else true
            orrFlag && operatorFlag && nameFlag && idFlag && srsFlag
        })
      val visited = locationService.getVisitedLocations(request.user)

      val visits: Map[String, Boolean] = locations.map({
        l =>
          l.id -> visited.contains(l.id)
      }).toMap

      val formActions:Map[String, Call] = locations.map({
        loc =>
          loc.id -> routes.ApiAuthenticatedController.visitLocationFromList(loc.id)
      }).toMap
      val locIds = locations.map{_.id}

      val visitedLocs = visited.count({ v => locIds.contains(v) })
      val availableLocs = locations.size
      val percentage = (visitedLocs.toDouble / availableLocs.toDouble) * 100.0
      val formattedPercentage: String = f"$percentage%1.1f"
      Ok(views.html.locations.groupedByTiploc(
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
