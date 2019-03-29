import models.auth.User
import models.route.{Route, RoutePoint, RoutesService}
import org.scalatest.{FlatSpec, Matchers}

class RoutesServiceTest extends FlatSpec with Matchers{

  val route = Route(RoutePoint(0.0, 0.0, "HWM", "Harlow Mill", ""), RoutePoint(0.0, 0.0, "SAW", "Sawbridgeworth", ""), "", "", "", "", "", "")
  val route2 = Route(RoutePoint(0.0, 0.0, "SAW", "Sawbridgeworth", ""), RoutePoint(0.0, 0.0, "BIS", "Bishops Stortford", ""), "", "", "", "", "", "")

  val user = User(1, "user", Set())
  val user2 = User(2, "user", Set())

  "Route Service" should "get no visits to a route if a user has none" in {
    val service = new RoutesService()

    val visits = service.getVisitsForRoute(route, user)

    visits should be(List())
  }

  it should "save a visit to a route" in {
    val service = new RoutesService()

    service.visitRoute(route, user)
    val visits = service.getVisitsForRoute(route, user)

    visits.size should be(1)
    visits.head should be(java.time.LocalDate.now.toString)
  }

  it should "save two visits to one route for one user" in {
    val service = new RoutesService()

    service.visitRoute(route, user)
    service.visitRoute(route, user)
    val visits = service.getVisitsForRoute(route, user)

    visits.size should be(2)
  }

  it should "save one visits to two route for one user" in {
    val service = new RoutesService()

    service.visitRoute(route, user)
    service.visitRoute(route2, user)
    val visits = service.getVisitsForRoute(route, user)
    val visits2 = service.getVisitsForRoute(route2, user)

    visits.size should be(1)
    visits2.size should be(1)
  }

  it should "save one visits to two route for two user" in {
    val service = new RoutesService()

    service.visitRoute(route, user)
    service.visitRoute(route2, user2)
    val visits = service.getVisitsForRoute(route, user)
    val visits2 = service.getVisitsForRoute(route2, user2)

    visits.size should be(1)
    visits2.size should be(1)
  }

  it should "delete last visit for a route when more than one visit" in {
    val service = new RoutesService()

    service.visitRoute(route, user)
    service.visitRoute(route, user)
    service.getVisitsForRoute(route, user).size should be(2)

    service.deleteLastVisit(route, user)
    service.getVisitsForRoute(route, user).size should be(1)

  }

  it should "delete last visit for a route when one visit" in {
    val service = new RoutesService()

    service.visitRoute(route, user)
    service.getVisitsForRoute(route, user).size should be(1)

    service.deleteLastVisit(route, user)
    service.getVisitsForRoute(route, user).size should be(0)
  }

  it should "delete last visit for a route when no visit" in {
    val service = new RoutesService()

    service.deleteLastVisit(route, user)
    service.getVisitsForRoute(route, user).size should be(0)
  }

  it should "delete last visit for one use only" in {
    val service = new RoutesService()

    service.visitRoute(route, user)
    service.visitRoute(route, user2)
    service.getVisitsForRoute(route, user).size should be(1)
    service.getVisitsForRoute(route, user2).size should be(1)

    service.deleteLastVisit(route, user)
    service.getVisitsForRoute(route, user).size should be(0)
    service.getVisitsForRoute(route, user2).size should be(1)
  }

  it should "delete all visits" in {
    val service = new RoutesService()

    service.visitRoute(route, user)
    service.visitRoute(route, user)
    service.getVisitsForRoute(route, user).size should be(2)

    service.deleteAllVisits(route, user)
    service.getVisitsForRoute(route, user).size should be(0)
  }
}
