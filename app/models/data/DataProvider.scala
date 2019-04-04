package models.data

import models.auth.User
import models.location.Location
import models.route.Route

trait DataProvider[T] {

  def getVisits(user: User): Option[Map[String, List[String]]]
  def saveVisit(id: T, user: User): Unit
  def saveVisits(visits: Option[Map[String, List[String]]], user: User)
  def removeLastVisit(id: T, user: User): Unit
  def removeAllVisits(id: T, user: User): Unit

  def idToString(id: T): String

  def timestamp(): String = java.time.LocalDate.now.toString
}

trait LocationDataProvider extends DataProvider[Location]

trait RouteDataProvider extends DataProvider[Route]
