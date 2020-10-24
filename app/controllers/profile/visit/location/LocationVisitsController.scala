package controllers.profile.visit.location

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import models.location.{Location, MapLocation}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.location.LocationService
import services.visit.location.LocationVisitService
import services.visit.route.RouteVisitService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@Singleton
class LocationVisitsController @Inject()(
                                          userDao: UserDao,
                                          jwtService: JWTService,
                                          cc: ControllerComponents,
                                          locationsService: LocationService,
                                          locationVisitsService: LocationVisitService,
                                          authenticatedUserAction: AuthorizedWebAction,
                                          authorizedAction: AuthorizedAction
                                        ) extends AbstractController(cc) {


  def index = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val visits: Map[String, List[String]] =
      locationVisitsService
        .getVisitsForUser(request.user)
        .getOrElse(Map.empty[String, List[String]])

    val visitedLocations: Map[String, Location] = visits
      .keySet
      .map(v => v -> locationsService.findFirstLocationByTiploc(v))
      .filter(_._2.nonEmpty)
      .map(v => v._1 -> v._2.get)
      .toMap

    val locationsAndVisits: List[(Location, List[String])] = visits
      .keys
      .map(key => {
        (visitedLocations(key), visits(key))
      })
      .toList

    val locationsAndVisitsIndex: scala.List[(String, Option[Int])] = getVisitsIndex(request, visits)
    println(locationsAndVisitsIndex.size)
    println(locationsAndVisitsIndex.head)

    val locationsVisitsAndIndex =  locationsAndVisits
      .zip(locationsAndVisitsIndex.map(_._2))
      .map(entry => (entry._1._1, entry._1._2, entry._2))
      .sortBy(_._3)

    println(locationsVisitsAndIndex.head)

    val mapLocations: List[MapLocation] = visits
      .keySet
      .map(v => visitedLocations(v))
      .map(MapLocation(_))
      .toList

    Ok(views.html.visits.location.index(request.user, locationsVisitsAndIndex, mapLocations))

  }

  private def getVisitsIndex(request: WebUserContext[AnyContent], visits: Map[String, List[String]]): List[(String, Option[Int])] = {
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
    try {
      println("Trying to get indexes")
      val locationsAndVisitIndexFF: List[Future[(String, Option[Int])]] = visits
        .keys
        .map(visit => Future(visit -> locationVisitsService.getStationVisitNumber(request.user, visit)))
        .toList


      val locationsAndVisitIndexF = Future.sequence(locationsAndVisitIndexFF)

      val locationsAndVisitsIndex = Await.result(locationsAndVisitIndexF, Duration("60 seconds"))
      locationsAndVisitsIndex
    }
    catch {
      case e: Exception => {
        println(s"Something went wrong: $e")
        List.empty
      }
    }
  }
}

