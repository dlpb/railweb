package models.route

import models.auth.User
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.collection.mutable
import scala.io.Source

class RoutesService {

  private val visits: scala.collection.mutable.Map[User, mutable.Map[Route, List[String]]] =
    new mutable.HashMap()
  
  private val routes = RoutesService.makeRoutes(RoutesService.readRoutesFromFile)

  def getRoute(fromId: String, toId: String): Option[Route] =
    routes.find(r => r.from.id.equals(fromId) &&  r.to.id.equals(toId))


  def mapRoutes: Set[MapRoute] = {
    routes map {l => MapRoute(l)}
  }

  def defaultListRoutes: Set[ListRoute] = {
    routes map {l => ListRoute(l)}
  }


  def getVisitsForRoute(route: Route, user: User): List[String] = {
    visits.get(user) flatMap {
      _.get(route)
    } match {
      case Some(list) => list
      case None => List()
    }
  }

  def visitRoute(route: Route, user: User): Unit = {
    val visitsForUser: Option[mutable.Map[Route, List[String]]] = visits.get(user)
    visitsForUser match {
      case Some(_) =>
        val visitsForRoute: Option[List[String]] = visits(user).get(route)
        visitsForRoute match {
          case Some(_) => visits(user)(route) = java.time.LocalDate.now.toString :: visits(user)(route)
          case None => visits(user)(route) = List(java.time.LocalDate.now.toString)
        }
      case None =>
        visits(user) = new mutable.HashMap()
        visitRoute(route, user)
    }
  }

  def deleteLastVisit(route: Route, user: User): Unit = {
    val visitsForUser: Option[mutable.Map[Route, List[String]]] = visits.get(user)
    visitsForUser match {
      case Some(_) =>
        val visitsForRoute: Option[List[String]] = visits(user).get(route)
        visitsForRoute match {
          case Some(_) => visits(user)(route) match {
            case _ :: tail => visits(user)(route) = tail
            case _ => visits(user)(route) = List()
          }
          case None =>
        }
      case None =>
    }
  }

  def deleteAllVisits(route: Route, user: User): Unit = {
    val visitsForUser: Option[mutable.Map[Route, List[String]]] = visits.get(user)
    visitsForUser match {
      case Some(_) =>
        val visitsForRoute: Option[List[String]] = visits(user).get(route)
        visitsForRoute match {
          case Some(_) => visits(user)(route) = List()
          case None =>
        }
      case None =>
    }
  }
}

object RoutesService {
  def readRoutesFromFile: String = {
    Source.fromFile(System.getProperty("user.dir") + "/resources/data/static/routes.json").mkString
  }

  def makeRoutes(routes: String): Set[Route] = {
    implicit val formats = DefaultFormats
    parse(routes).extract[Set[Route]]
  }
}