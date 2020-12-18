package controllers.location.list.crs

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.{GroupedListLocation, ListLocation}
import play.api.mvc._
import services.location.LocationService
import services.visit.location.LocationVisitService
@Singleton
class LocationsByCrsController @Inject()(
  cc: ControllerComponents,
  authenticatedUserAction: AuthorizedWebAction,
  locationService: LocationService,
  locationVisitService: LocationVisitService,
  jwtService: JWTService
) extends AbstractController(cc) {

  def getListOfLocations(filterOrr: Boolean = false,
                         filterOperator: String = "all",
                         filterName: String = "all",
                         filterId: String = "all",
                         filterSrs: String = "all"): List[GroupedListLocation] =
    locationService.sortedListLocationsGroupedByCrs
      .filter({ location =>
        val filterByOrr = if (filterOrr) location.isOrrStation else true
        val filterByOperator = if(!filterOperator.equals("all")) location.operator.toLowerCase.contains(filterOperator.toLowerCase()) else true
        val filterByName = if(!filterName.equals("all")) location.name.toLowerCase.contains(filterName.toLowerCase()) else true
        val filterById = if(!filterId.equals("all")) location.id.toLowerCase().contains(filterId.toLowerCase()) else true
        val filterBySrs = if(!filterSrs.equals("all")) location.srs.toLowerCase.contains(filterSrs.toLowerCase()) else true
        filterByOrr && filterByOperator && filterByName && filterById && filterBySrs
      })

  def getVisitStatus(locations: List[GroupedListLocation], visitedIds: List[String]): Map[String, Boolean] =
    locations
    .map({ l =>
      l.id -> {
        val relatedIds = l.relatedLocations.map(_.id)
        val isRelatedIdVisited = relatedIds.exists(visitedIds.contains(_))
        isRelatedIdVisited
      }
    })
    .toMap

  def getVisitedLocationIdCount(locations: List[GroupedListLocation], visitedLocationIds: List[String]) = {

    val locIds = locations.flatMap {
      _.relatedLocations.map(_.id)
    }

    val visitedLocs = visitedLocationIds.count({ v =>
      locIds.contains(v)
    })

    visitedLocs
  }

  def calculatePercentage(visitedCount: Double, totalCount: Double) = {
    if(totalCount.equals(0.0)) "0"
    else {
      val percentage = (visitedCount.toDouble / totalCount.toDouble) * 100.0
      val formattedPercentage: String = f"$percentage%1.1f"

      formattedPercentage
    }

  }

  def index(orr: Boolean,
            operator: String,
            name: String,
            id: String,
            srs: String) = authenticatedUserAction {
    implicit request: WebUserContext[AnyContent] =>
      if (request.user.roles.contains(MapUser)) {
        val token = jwtService.createToken(request.user, new Date())
        val locationTiplocs =
          locationService.sortedListLocationsGroupedByCrs.map(_.id)
        val locations: List[GroupedListLocation] = getListOfLocations(orr, operator, name, id, srs)
        val visited = locationVisitService.getVisitedLocations(request.user)
        val groupedVisited: List[String] =
          locationVisitService.getVisitedLocationsByCrs(request.user)

        val groupVisits: Map[String, Boolean] = getVisitStatus(locations, groupedVisited)
        val visits: Map[String, Boolean] = getVisitStatus(locations, visited)

        val formActions: Map[String, Call] = locationTiplocs
          .map({ loc =>
            loc -> controllers.api.locations.visit.routes.VisitLocationsApiController
              .visitLocationFromCrsList(loc)
          })
          .toMap
        val locIds = locations.map {
          _.id
        }

        val visitedLocationIdCount: Int = getVisitedLocationIdCount(locations, groupedVisited)

        val availableLocs = locations.size
        val formattedPercentage: String = calculatePercentage(visitedLocationIdCount.toDouble, availableLocs.toDouble)
        Ok(
          views.html.locations.list.crs.index(
            request.user,
            locations,
            visits,
            groupVisits,
            formActions,
            token,
            visitedLocationIdCount,
            availableLocs,
            formattedPercentage,
            orr,
            name,
            id,
            operator,
            srs
          )
        )
      } else {
        Forbidden("User not authorized to view page")
      }
  }

}
