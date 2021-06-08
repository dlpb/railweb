import java.nio.file.Files

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import data.RouteMapBasedDataProvider
import models.auth.User
import models.data.{LocationJsonFileVisitDataProvider, RouteJsonFileVisitDataProvider}
import models.location.{Location, SpatialLocation}
import models.route.{Route, RoutePoint}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.FlatSpec
import org.scalatest.matchers.should.Matchers

class FileBasedDataStorageTests extends AnyFlatSpec with Matchers {

  val location = Location("LST", "Liverpool Street", "", "", SpatialLocation(0.0, 0.0, None, None, None),None, true, Set(), Set())
  val location2 = Location("KGX", "Kings Cross", "", "", SpatialLocation(0.0, 0.0, None, None, None),None, true, Set(), Set())
  val user = User(1, "user", Set())
  val user2 = User(2, "user", Set())

  val route = Route(RoutePoint(0.0, 0.0, "HWM", "Harlow Mill", ""), RoutePoint(0.0, 0.0, "SAW", "Sawbridgeworth", ""), "", "", "", "", "", "")
  val route2 = Route(RoutePoint(0.0, 0.0, "SAW", "Sawbridgeworth", ""), RoutePoint(0.0, 0.0, "BIS", "Bishops Stortford", ""), "", "", "", "", "", "")

  "file storage"  should "get no visits to a location if a user has none" in {
      val storage = new  LocationJsonFileVisitDataProvider(config)

      val visits = storage.getVisits(user)

      visits should be(None)
    }

    it should "save a visit to a location" in {
      val storage = new  LocationJsonFileVisitDataProvider(config)
      storage.saveVisit(location, user)
      val visits = storage.getVisits(user).get

      visits(storage.idToString(location)).size should be(1)
      visits(storage.idToString(location)).head should be(java.time.LocalDate.now.toString)
    }

    it should "save two visits to one location for one user" in {
      val storage = new  LocationJsonFileVisitDataProvider(config)
      storage.saveVisit(location, user)
      storage.saveVisit(location, user)
      val visits = storage.getVisits(user).get

      visits(storage.idToString(location)).size should be(2)
    }

    it should "save one visits to two location for one user" in {
      val storage = new  LocationJsonFileVisitDataProvider(config)

      storage.saveVisit(location, user)
      storage.saveVisit(location2, user)
      val visits = storage.getVisits(user).get

      visits(storage.idToString(location)).size should be(1)
      visits(storage.idToString(location2)).size should be(1)
    }

    it should "save one visits to two location for two user" in {
      val storage = new  LocationJsonFileVisitDataProvider(config)

      storage.saveVisit(location, user)
      storage.saveVisit(location2, user2)

      storage.getVisits(user).get(storage.idToString(location)).size should be(1)
      storage.getVisits(user2).get(storage.idToString(location2)).size should be(1)
    }

    it should "delete last visit for a location when more than one visit" in {
      val storage = new  LocationJsonFileVisitDataProvider(config)

      storage.saveVisit(location, user)
      storage.saveVisit(location, user)
      storage.getVisits(user).get(storage.idToString(location)).size should be(2)

      storage.removeLastVisit(location, user)
      storage.getVisits(user).get(storage.idToString(location)).size should be(1)
    }

    it should "delete last visit for a location when one visit" in {
      val storage = new  LocationJsonFileVisitDataProvider(config)

      storage.saveVisit(location, user)
      storage.getVisits(user).get(storage.idToString(location)).size should be(1)

      storage.removeLastVisit(location, user)
      storage.getVisits(user).get(storage.idToString(location)).size should be(0)
    }

    it should "delete last visit for a location when no visit" in {
      val storage = new  LocationJsonFileVisitDataProvider(config)

      storage.removeLastVisit(location, user)
      storage.getVisits(user).get should be(Map())
    }

    it should "delete last visit for one use only" in {
      val storage = new  LocationJsonFileVisitDataProvider(config)

      storage.saveVisit(location, user)
      storage.saveVisit(location, user2)
      storage.getVisits(user).get(storage.idToString(location)).size should be(1)
      storage.getVisits(user2).get(storage.idToString(location)).size should be(1)

      storage.removeLastVisit(location, user)
      storage.getVisits(user).get(storage.idToString(location)).size should be(0)
      storage.getVisits(user2).get(storage.idToString(location)).size should be(1)
    }

    it should "delete all visits" in {
      val storage = new  LocationJsonFileVisitDataProvider(config)

      storage.saveVisit(location, user)
      storage.saveVisit(location, user)
      storage.getVisits(user).get(storage.idToString(location)).size should be(2)

      storage.removeAllVisits(location, user)
      storage.getVisits(user).get(storage.idToString(location)).size should be(0)
    }

  it should "migrate visited route to another pair of routes" in {
    val storage = new  RouteJsonFileVisitDataProvider(config)

    val migratedRoutePart1 = Route(RoutePoint(0.0, 0.0, "HWM", "Harlow Mill", ""), RoutePoint(0.0, 0.0, "Midpoint", "HWMSBWMidpoint", ""), "", "", "", "", "", "")
    val migratedRoutePart2 = Route(RoutePoint(0.0, 0.0, "Midpoint", "HWMSBWMidpoint", ""), RoutePoint(0.0, 0.0, "SAW", "Sawbridgeworth", ""), "", "", "", "", "", "")

    storage.saveVisit(route, user)
    storage.saveVisit(route, user2)
    storage.getVisits(user).size should be(1)
    storage.getVisits(user2).size should be(1)

    storage.migrate(user, route, List(migratedRoutePart1, migratedRoutePart2))

    val migratedData = storage.getVisits(user).get
    migratedData("from:HWM-to:Midpoint").size should be(1)
    migratedData("from:Midpoint-to:SAW").size should be(1)
    migratedData.size should be(2)

  }

  it should "should not remove unmigrated routes" in {
    val storage = new  RouteJsonFileVisitDataProvider(config)

    val migratedRoutePart1 = Route(RoutePoint(0.0, 0.0, "HWM", "Harlow Mill", ""), RoutePoint(0.0, 0.0, "Midpoint", "HWMSBWMidpoint", ""), "", "", "", "", "", "")
    val migratedRoutePart2 = Route(RoutePoint(0.0, 0.0, "Midpoint", "HWMSBWMidpoint", ""), RoutePoint(0.0, 0.0, "SAW", "Sawbridgeworth", ""), "", "", "", "", "", "")

    storage.saveVisit(route, user)
    storage.saveVisit(route2, user)
    storage.getVisits(user).get.size should be(2)

    storage.migrate(user, route, List(migratedRoutePart1, migratedRoutePart2))

    val migratedData = storage.getVisits(user).get
    migratedData("from:HWM-to:Midpoint").size should be(1)
    migratedData("from:Midpoint-to:SAW").size should be(1)
    migratedData("from:SAW-to:BIS").size should be(1)
    migratedData.size should be(3)

  }

  def config = ConfigFactory
    .empty()
    .withValue("data.user.root", ConfigValueFactory.fromAnyRef(Files.createTempDirectory("test").toFile.getAbsolutePath))
}
