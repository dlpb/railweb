package services.location

import javax.inject.{Inject, Singleton}
import models.location.Location
import models.route.RoutePoint
import services.route.RouteService
import services.visit.location.PathElementLocation

@Singleton
class AdjacentLocationService @Inject() (locationService: LocationService, routeService: RouteService){
  def findAdjacentLocations(startingPoint: Location,
                            orrStationMaxDepth: Int = 2,
                            nonOrrStationMaxDepth: Int = 2,
                            countNonOrrStations: Boolean = false,
                            path: List[PathElementLocation] = List.empty,
                            visitedLocIds: List[String] = List.empty
                           ): List[PathElementLocation] = {

    def findAdjacentLocations0(startingPoint: Location,
                               orrStationMaxDepth: Int,
                               nonOrrStationMaxDepth: Int,
                               countNonOrrStations: Boolean,
                               path: List[PathElementLocation],
                               visitedLocIds: List[String]
                              ): List[PathElementLocation] = {


      if (orrStationMaxDepth == 0 || nonOrrStationMaxDepth == 0) {
        path
      }
      else {
        val routesForLocation = routeService.findRoutesForLocation(startingPoint)
        val endPoints: List[RoutePoint] = routesForLocation
          .map(r => {
            if (r.from.id.equals(startingPoint.id)) r.to else r.from
          })
          .filterNot(endpoint => visitedLocIds.contains(endpoint.id))
          .toList


        val adjacentPathElements: List[PathElementLocation] = endPoints.map(endpoint => {
          val endpointLoc = locationService.findFirstLocationByNameTiplocCrsOrId(endpoint.id).get

          val isOrrStation = endpointLoc.isOrrStation
          val newOrrStationMaxDepth = if (isOrrStation) orrStationMaxDepth - 1 else orrStationMaxDepth
          val newNonOrrStationMaxDepth = if (isOrrStation) nonOrrStationMaxDepth else nonOrrStationMaxDepth - 1
          val newVisitedLocIds = visitedLocIds :+ endpointLoc.id
          val newPath = path.filterNot(p => visitedLocIds.contains(p.location.id))

          val adjacent = PathElementLocation(endpointLoc, findAdjacentLocations0(endpointLoc, newOrrStationMaxDepth, newNonOrrStationMaxDepth, countNonOrrStations, newPath, newVisitedLocIds))
          adjacent
        })

        val pathElements = adjacentPathElements

        pathElements

      }
    }
    val result = List(PathElementLocation(startingPoint, findAdjacentLocations0(startingPoint, orrStationMaxDepth, nonOrrStationMaxDepth, countNonOrrStations, List(PathElementLocation(startingPoint, List.empty)), List(startingPoint.id))))
    result
  }
}
