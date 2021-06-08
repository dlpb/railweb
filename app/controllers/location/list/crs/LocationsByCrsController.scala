package controllers.location.list.crs

import java.util.Date

import auth.JWTService
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.roles.MapUser
import models.location.{GroupedListLocation, Location}
import play.api.mvc._
import services.location.LocationService
import services.visit.event.EventService
import services.visit.location.LocationVisitService
@Singleton
class LocationsByCrsController @Inject()(
  cc: ControllerComponents,
  authenticatedUserAction: AuthorizedWebAction,
  locationService: LocationService,
  locationVisitService: LocationVisitService,
  eventService: EventService,
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

  def getVisitStatus(locations: List[Location], visitedIds: List[String]): Map[String, Boolean] =
    locations
    .map({ l =>
      l.id -> visitedIds.contains(l.id)
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

        val tiplocToCall: Map[String, Call] = locationService.locations
          .map({ loc =>
            if(loc.crs.nonEmpty)
              loc.id -> Some(controllers.api.locations.visit.routes.VisitLocationsApiController
                .visitLocationFromCrsList(loc.crs.head))
            else
              loc.id -> None
          })
          .filter(_._2.nonEmpty)
          .map(call => call._1 -> call._2.get)
          .toMap


        val locations = locationService.locations

        val locationsGroupedByCrs = locationService.sortedListLocationsGroupedByCrs

        val visitedLocationTiplocs = locationVisitService
          .getVisitedLocations(request.user)
          .map(_.id)

        val visitedLocationGroupsByCrs: Map[GroupedListLocation, Boolean] = locationsGroupedByCrs
            .map(groupedLocation => {
              val tiplocsForGroup = groupedLocation.relatedLocations.map(_.id)
              val hasAnyTiplocInGroupBeenVisited = tiplocsForGroup.map(visitedLocationTiplocs.contains(_))
              val hasGroupBeenVisited = hasAnyTiplocInGroupBeenVisited.exists(visited => visited)
              groupedLocation -> hasGroupBeenVisited
            }).toMap

        val crsGroupToVisitedStatus: Map[String, Boolean] = visitedLocationGroupsByCrs.iterator.map(visitedGroup => visitedGroup._1.id -> visitedGroup._2).toMap

        val numberOfTiplocsInCrsGroupVisited: Map[String, Int] = locationsGroupedByCrs.map(group => {
          val tiplocs = group.relatedLocations.map(_.id)
          val visitedTiploc = tiplocs.map(visitedLocationTiplocs.contains(_))
          val visitedCount = visitedTiploc.count(v => v)
          group.id -> visitedCount
        }).toMap

        val tiplocToVisitedStatus = locations
          .map(_.id)
          .map(location => location -> visitedLocationTiplocs.contains(location))
          .toMap

        val visitedLocationCount = getVisitedLocationIdCount(locationsGroupedByCrs, visitedLocationTiplocs)

        val availableLocs = locationsGroupedByCrs.size

        val formattedPercentage = calculatePercentage(visitedLocationCount.toDouble, availableLocs.toDouble)

        Ok(
          views.html.locations.list.crs.index(
            request.user,
            locationsGroupedByCrs,
            tiplocToVisitedStatus,
            crsGroupToVisitedStatus,
            numberOfTiplocsInCrsGroupVisited,
            tiplocToCall,
            token,
            visitedLocationCount,
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
