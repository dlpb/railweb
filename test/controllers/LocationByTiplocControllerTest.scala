package controllers

import auth.JWTService
import auth.web.AuthorizedWebAction
import com.typesafe.config.Config
import controllers.location.list.tiploc.LocationsByTiplocController
import data.LocationMapBasedDataProvider
import models.auth.User
import org.mockito.{Mock, MockitoSugar}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.mvc.ControllerComponents
import services.location.LocationService
import services.route.RouteService
import services.visit.location.LocationVisitService

class LocationByTiplocControllerTest
    extends AnyFlatSpec
    with Matchers
    with MockitoSugar {

  val mockControllerComponets = mock[ControllerComponents]
  val mockAuthenticatedUserAction = mock[AuthorizedWebAction]
  val mockJWTService = mock[JWTService]

  "Location By Tiploc Controller" should "return a list of locations unfiltered" in {
    val locationService = getLocationServiceWith("/twoCrsWithStation.json")
    val locationVisitService = getLocationVisitServiceWith(mock[Config], locationService)

    val controller = new LocationsByTiplocController(
      mockControllerComponets,
      mockAuthenticatedUserAction,
      locationService,
      locationVisitService,
      mockJWTService
    )

    val locations = controller.getListOfLocations()

    locations.size should be(3)

  }

  it should "show only official stations if filtered by ORR" in {
    val locationService = getLocationServiceWith("/twoCrsOneNotStation.json")
    val locationVisitService = getLocationVisitServiceWith(mock[Config], locationService)

    val controller = new LocationsByTiplocController(
      mockControllerComponets,
      mockAuthenticatedUserAction,
      locationService,
      locationVisitService,
      mockJWTService
    )

    val locations = controller.getListOfLocations(filterOrr = true)

    locations.size should be(1)
  }

  it should "only show stations filtered by operator if there is an operator other than all" in {
    val locationService = getLocationServiceWith("/adjacentLocations.json")
    val locationVisitService = getLocationVisitServiceWith(mock[Config], locationService)

    val controller = new LocationsByTiplocController(
      mockControllerComponets,
      mockAuthenticatedUserAction,
      locationService,
      locationVisitService,
      mockJWTService
    )

    val locations = controller.getListOfLocations(filterOperator = "LO")

    locations.size should be(1)
  }

  it should "only show stations filtered by name if there is an name other than all" in {
    val locationService = getLocationServiceWith("/adjacentLocations.json")
    val locationVisitService = getLocationVisitServiceWith(mock[Config], locationService)

    val controller = new LocationsByTiplocController(
      mockControllerComponets,
      mockAuthenticatedUserAction,
      locationService,
      locationVisitService,
      mockJWTService
    )

    val locations = controller.getListOfLocations(filterName = "Emerson Park")

    locations.size should be(1)
  }

  it should "only show stations filtered by ID when the id flag is not equal to all" in {
    val locationService = getLocationServiceWith("/twoCrsOneNotStation.json")
    val locationVisitService = getLocationVisitServiceWith(mock[Config], locationService)

    val controller = new LocationsByTiplocController(
      mockControllerComponets,
      mockAuthenticatedUserAction,
      locationService,
      locationVisitService,
      mockJWTService
    )

    val locations = controller.getListOfLocations(filterId = "LST")

    locations.size should be(1)
  }

  it should "only show stations filtered by srs if there is an srs other than all" in {
    val locationService = getLocationServiceWith("/adjacentLocations.json")
    val locationVisitService = getLocationVisitServiceWith(mock[Config], locationService)

    val controller = new LocationsByTiplocController(
      mockControllerComponets,
      mockAuthenticatedUserAction,
      locationService,
      locationVisitService,
      mockJWTService
    )

    val locations = controller.getListOfLocations(filterSrs = "D.17")

    locations.size should be(1)
  }

  it should "get visit status for each location" in {
    val locationService = getLocationServiceWith("/adjacentLocations.json")
    val locationVisitService = getLocationVisitServiceWith(mock[Config], locationService)

    val controller = new LocationsByTiplocController(
      mockControllerComponets,
      mockAuthenticatedUserAction,
      locationService,
      locationVisitService,
      mockJWTService
    )

    val location = locationService.locations.head
    locationVisitService.visitLocation(location, User(1, "user", Set.empty))
    val visitedIds = locationVisitService.getVisitedLocations(User(1, "user", Set.empty))

    val visitStatus = controller.getVisitStatus(locationService.sortedListLocationsGroupedByTiploc, visitedIds)

    visitStatus.count(_._2) should be(1)

  }

  it should "get all visited locations" in {
    val locationService = getLocationServiceWith("/adjacentLocations.json")
    val locationVisitService = getLocationVisitServiceWith(mock[Config], locationService)

    val controller = new LocationsByTiplocController(
      mockControllerComponets,
      mockAuthenticatedUserAction,
      locationService,
      locationVisitService,
      mockJWTService
    )

    val location = locationService.locations.head
    locationVisitService.visitLocation(location, User(1, "user", Set.empty))
    val visitedIds = locationVisitService.getVisitedLocations(User(1, "user", Set.empty))

    val visitedLocationsCount = controller.getVisitedLocationIdCount(locationService.sortedListLocationsGroupedByTiploc, visitedIds)

    visitedLocationsCount should be(1)
  }

  it should "calculate and format a percentage if there is a non zero number of visited locations" in {
    val locationService = getLocationServiceWith("/adjacentLocations.json")
    val locationVisitService = getLocationVisitServiceWith(mock[Config], locationService)

    val controller = new LocationsByTiplocController(
      mockControllerComponets,
      mockAuthenticatedUserAction,
      locationService,
      locationVisitService,
      mockJWTService
    )

    val location = locationService.locations.head
    locationVisitService.visitLocation(location, User(1, "user", Set.empty))
    val visitedIds = locationVisitService.getVisitedLocations(User(1, "user", Set.empty))

    val visitedLocationsCount = controller.getVisitedLocationIdCount(locationService.sortedListLocationsGroupedByTiploc, visitedIds)

    val percentage = controller.calculatePercentage(visitedLocationsCount, controller.getListOfLocations().size)

    percentage should be("8.3")
  }

  it should "calculate and format a percentage if there is a  zero number of total locations" in {
    val locationService = getLocationServiceWith("/adjacentLocations.json")
    val locationVisitService = getLocationVisitServiceWith(mock[Config], locationService)

    val controller = new LocationsByTiplocController(
      mockControllerComponets,
      mockAuthenticatedUserAction,
      locationService,
      locationVisitService,
      mockJWTService
    )

    val location = locationService.locations.head
    locationVisitService.visitLocation(location, User(1, "user", Set.empty))
    val visitedIds = locationVisitService.getVisitedLocations(User(1, "user", Set.empty))

    val visitedLocationsCount = controller.getVisitedLocationIdCount(locationService.sortedListLocationsGroupedByTiploc, visitedIds)

    val percentage = controller.calculatePercentage(visitedLocationsCount, 0.0)

    percentage should be("0")
  }

  def getLocationServiceWith(routePath: String) = {
    val mockConfig = mock[Config]
    when(mockConfig.getString("data.locations.path")).thenReturn(routePath)
    val service = new LocationService(mockConfig)
    service
  }

  def getLocationVisitServiceWith(config: Config, locationService: LocationService) = {
    val service = new LocationVisitService(config,
      locationService,
      new LocationMapBasedDataProvider())
    service
  }

}
