package models.data

import models.auth.User

trait DataProvider[T] {

  def getVisits(user: User): Option[Map[String, List[String]]]
  def saveVisit(id: T, user: User): Unit
  def removeLastVisit(id: T, user: User): Unit
  def removeAllVisits(id: T, user: User): Unit

  def idToString(id: T): String
}
