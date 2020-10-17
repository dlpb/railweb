//import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
//import data.LocationMapBasedDataProvider
//import models.auth.User
//import models.location.{Location, LocationsService, SpatialLocation}
//import org.scalatest.{FlatSpec, Matchers}
//
//class LocationServiceTest extends FlatSpec with Matchers {
//
//  val location = Location("LST", "Liverpool Street", "", "", SpatialLocation(0.0, 0.0, None, None, None),None, true, Set(), Set())
//  val location2 = Location("KGX", "Kings Cross", "", "", SpatialLocation(0.0, 0.0, None, None, None),None, true, Set(), Set())
//  val user = User(1, "user", Set())
//  val user2 = User(2, "user", Set())
//
//  "Location Service" should "get no visits to a location if a user has none" in {
//    val service = new LocationsService(config,
//      new LocationMapBasedDataProvider())
//
//    val visits = service.getVisitsForLocation(location, user)
//
//    visits should be(List())
//  }
//
//  it should "save a visit to a location" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//
//    service.visitLocation(location, user)
//    val visits = service.getVisitsForLocation(location, user)
//
//    visits.size should be(1)
//    visits.head should be(java.time.LocalDate.now.toString)
//  }
//
//  it should "save two visits to one location for one user" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//
//    service.visitLocation(location, user)
//    service.visitLocation(location, user)
//    val visits = service.getVisitsForLocation(location, user)
//
//    visits.size should be(2)
//  }
//
//  it should "save one visits to two location for one user" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//
//    service.visitLocation(location, user)
//    service.visitLocation(location2, user)
//    val visits = service.getVisitsForLocation(location, user)
//    val visits2 = service.getVisitsForLocation(location2, user)
//
//    visits.size should be(1)
//    visits2.size should be(1)
//  }
//
//  it should "save one visits to two location for two user" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//
//    service.visitLocation(location, user)
//    service.visitLocation(location2, user2)
//    val visits = service.getVisitsForLocation(location, user)
//    val visits2 = service.getVisitsForLocation(location2, user2)
//
//    visits.size should be(1)
//    visits2.size should be(1)
//  }
//
//  it should "delete last visit for a location when more than one visit" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//
//    service.visitLocation(location, user)
//    service.visitLocation(location, user)
//    service.getVisitsForLocation(location, user).size should be(2)
//
//    service.deleteLastVisit(location, user)
//    service.getVisitsForLocation(location, user).size should be(1)
//
//  }
//
//  it should "delete last visit for a location when one visit" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//
//    service.visitLocation(location, user)
//    service.getVisitsForLocation(location, user).size should be(1)
//
//    service.deleteLastVisit(location, user)
//    service.getVisitsForLocation(location, user).size should be(0)
//  }
//
//  it should "delete last visit for a location when no visit" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//
//    service.deleteLastVisit(location, user)
//    service.getVisitsForLocation(location, user).size should be(0)
//  }
//
//  it should "delete last visit for one use only" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//
//    service.visitLocation(location, user)
//    service.visitLocation(location, user2)
//    service.getVisitsForLocation(location, user).size should be(1)
//    service.getVisitsForLocation(location, user2).size should be(1)
//
//    service.deleteLastVisit(location, user)
//    service.getVisitsForLocation(location, user).size should be(0)
//    service.getVisitsForLocation(location, user2).size should be(1)
//  }
//
//  it should "delete all visits" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//
//    service.visitLocation(location, user)
//    service.visitLocation(location, user)
//    service.getVisitsForLocation(location, user).size should be(2)
//
//    service.deleteAllVisits(location, user)
//    service.getVisitsForLocation(location, user).size should be(0)
//  }
//
//  it should "work out visited locations for an event" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//    service.visitLocation(service.getLocation("GFORDSJ").get, user)
//    service.visitLocation(service.getLocation("GFORDEJ").get, user)
//    service.visitLocation(service.getLocation("PROY").get, user)
//
//    val visitedLocations = service.getLocationsVisitedForEvent(new LocationMapBasedDataProvider().timestamp(), user)
//    visitedLocations.size should be(3)
//  }
//
//  it should "find a location by tiploc" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//    service.findLocationByNameTiplocCrsOrId("LIVST").get.id should be("LST")
//  }
//
//  it should "find a location by crs" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//    service.findLocationByNameTiplocCrsOrId("LST").get.id should be("LST")
//  }
//
//  it should "find a location by name" in {
//    val service = new LocationsService(config, new LocationMapBasedDataProvider())
//    service.findLocationByNameTiplocCrsOrId("London Liverpool Street").get.id should be("LST")
//  }
//
//
//  private def config = {
//    val config = ConfigFactory
//      .empty()
//      .withValue("data.static.root", ConfigValueFactory.fromAnyRef(getClass().getResource("locations.json").getPath))
//    config
//  }
//
//}
