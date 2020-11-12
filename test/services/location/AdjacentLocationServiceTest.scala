package services.location

import com.typesafe.config.Config
import models.location.Location
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import services.route.RouteService

class AdjacentLocationServiceTest
    extends AnyFlatSpec
    with Matchers
    with MockitoSugar {

  "Adjacent Location Service" should "return a list of all locations in the json file" in {
    val locationService = getLocationServiceWith("/adjacentLocations.json")
    val routeService = getRouteServiceWith("/adjacentRoutes.json")
    val startingLocation = locationService.findFirstLocationByIdOrCrs("CHDWLHT").get

    val service: AdjacentLocationService = getAdjacentLocationServiceWith(locationService, routeService)

    val adjacentLocations = service.findAdjacentLocations(startingLocation)

    adjacentLocations.size should be(1)
    adjacentLocations.map(_.location.id) should be(List("CHDWLHT"))

    adjacentLocations.head.adjacentPathElements.size should be(2)
    adjacentLocations.head.adjacentPathElements.map(_.location.id) should be(List("GODMAYS", "CHDWHTT"))

    adjacentLocations.head.adjacentPathElements.head.adjacentPathElements.map(_.location.id) should be(List("SVNKNGS"))
  }

  def getLocationServiceWith(routePath: String) = {
    val mockConfig = mock[Config]
    when(mockConfig.getString("data.locations.path")).thenReturn(routePath)
    val service = new LocationService(mockConfig)
    service
  }

  def getRouteServiceWith(routePath: String) = {
    val mockConfig = mock[Config]
    when(mockConfig.getString("data.routes.path")).thenReturn(routePath)
    val service = new RouteService(mockConfig)
    service
  }

  private def getAdjacentLocationServiceWith(locationService: LocationService,
                                             routeService: RouteService): AdjacentLocationService = {
    new AdjacentLocationService(locationService, routeService)

  }
}
