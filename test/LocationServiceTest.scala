import models.auth.User
import models.location.{Location, LocationsService, SpatialLocation}
import org.scalatest.{FlatSpec, Matchers}

class LocationServiceTest extends FlatSpec with Matchers {

  val location = Location("LST", "Liverpool Street", "", "", SpatialLocation(0.0, 0.0),None, true, Set(), Set())
  val location2 = Location("KGX", "Kings Cross", "", "", SpatialLocation(0.0, 0.0),None, true, Set(), Set())
  val user = User(1, "user", Set())
  val user2 = User(2, "user", Set())

  "Location Service" should "get no visits to a location if a user has none" in {
    val service = new LocationsService()

    val visits = service.getVisitsForLocation(location, user)

    visits should be(List())
  }

  it should "save a visit to a location" in {
    val service = new LocationsService()

    service.visitLocation(location, user)
    val visits = service.getVisitsForLocation(location, user)

    visits.size should be(1)
  }

  it should "save two visits to one location for one user" in {
    val service = new LocationsService()

    service.visitLocation(location, user)
    service.visitLocation(location, user)
    val visits = service.getVisitsForLocation(location, user)

    visits.size should be(2)
  }

  it should "save one visits to two location for one user" in {
    val service = new LocationsService()

    service.visitLocation(location, user)
    service.visitLocation(location2, user)
    val visits = service.getVisitsForLocation(location, user)
    val visits2 = service.getVisitsForLocation(location2, user)

    visits.size should be(1)
    visits2.size should be(1)
  }

  it should "save one visits to two location for two user" in {
    val service = new LocationsService()

    service.visitLocation(location, user)
    service.visitLocation(location2, user2)
    val visits = service.getVisitsForLocation(location, user)
    val visits2 = service.getVisitsForLocation(location2, user2)

    visits.size should be(1)
    visits2.size should be(1)
  }
}
