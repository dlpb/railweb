package models.location

import models.auth.User
import org.json4s.jackson.JsonMethods._
import org.json4s._

import scala.collection.mutable
import scala.io.Source

class LocationsService {


  private val visits: scala.collection.mutable.Map[User, mutable.Map[Location, List[String]]] =
    new mutable.HashMap()

  private val locations = LocationsService.makeLocations(LocationsService.readLocationsFromFile)

  def getLocation(id: String): Option[Location] =
    locations.find(_.id.equals(id))


  def mapLocations: Set[MapLocation] = {
    locations map { l => MapLocation(l) }
  }

  def defaultListLocations: Set[ListLocation] = {
    locations map { l => ListLocation(l) }
  }

  def getVisitsForLocation(location: Location, user: User): List[String] = {
    visits.get(user) flatMap {
      _.get(location)
    } match {
      case Some(list) => list
      case None => List()
    }
  }

  def visitLocation(location: Location, user: User): Unit = {
    val visitsForUser: Option[mutable.Map[Location, List[String]]] = visits.get(user)
    visitsForUser match {
      case Some(_) =>
        val visitsForLocation: Option[List[String]] = visits(user).get(location)
        visitsForLocation match {
          case Some(_) => visits(user)(location) = java.time.LocalDate.now.toString :: visits(user)(location)
          case None => visits(user)(location) = List(java.time.LocalDate.now.toString)
        }
      case None =>
        visits(user) = new mutable.HashMap()
        visitLocation(location, user)
    }
  }

  def deleteLastVisit(location: Location, user: User): Unit = {
    val visitsForUser: Option[mutable.Map[Location, List[String]]] = visits.get(user)
    visitsForUser match {
      case Some(_) =>
        val visitsForLocation: Option[List[String]] = visits(user).get(location)
        visitsForLocation match {
          case Some(_) => visits(user)(location) match {
            case _ :: tail => visits(user)(location) = tail
            case _ => visits(user)(location) = List()
          }
          case None =>
        }
      case None =>
    }
  }

  def deleteAllVisit(location: Location, user: User): Unit = {
    val visitsForUser: Option[mutable.Map[Location, List[String]]] = visits.get(user)
    visitsForUser match {
      case Some(_) =>
        val visitsForLocation: Option[List[String]] = visits(user).get(location)
        visitsForLocation match {
          case Some(_) => visits(user)(location) = List()
          case None =>
        }
      case None =>
    }
  }
}

object LocationsService {

  def readLocationsFromFile: String = {
    Source.fromFile(System.getProperty("user.dir") + "/resources/data/static/locations.json").mkString
  }

  def makeLocations(locations: String): Set[Location] = {
    implicit val formats = DefaultFormats
    parse(locations).extract[Set[Location]]
  }
}