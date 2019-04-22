package data

import models.auth.User
import models.data.DataProvider

import scala.collection.mutable

trait MapBasedStorageProvider[T] extends DataProvider[T] {
  private val visits: scala.collection.mutable.Map[User, mutable.Map[String, List[String]]] =
    new mutable.HashMap()

  def idToString(id: T): String

  def saveVisit(id: T, user: User): Unit = {
    val visitsForUser: Option[mutable.Map[String, List[String]]] = visits.get(user)
    visitsForUser match {
      case Some(_) =>
        val visitsForLocation: Option[List[String]] = visits(user).get(idToString(id))
        visitsForLocation match {
          case Some(_) => visits(user)(idToString(id)) = timestamp() :: visits(user)(idToString(id))
          case None => visits(user)(idToString(id)) = List(timestamp())
        }
      case None =>
        visits(user) = new mutable.HashMap()
        saveVisit(id, user)
    }
  }

  def getVisits(user: User): Option[Map[String, List[String]]] = {
    visits.get(user) map { x => x.toMap }
  }

  def removeLastVisit(id: T, user: User): Unit = {
    val visitsForUser: Option[mutable.Map[String, List[String]]] = visits.get(user)
    visitsForUser match {
      case Some(_) =>
        val visitsForLocation: Option[List[String]] = visits(user).get(idToString(id))
        visitsForLocation match {
          case Some(_) => visits(user)(idToString(id)) match {
            case _ :: tail => visits(user)(idToString(id)) = tail
            case _ => visits(user)(idToString(id)) = List()
          }
          case None =>
        }
      case None =>
    }
  }

  def removeAllVisits(id: T, user: User): Unit = {
    val visitsForUser: Option[mutable.Map[String, List[String]]] = visits.get(user)
    visitsForUser match {
      case Some(_) =>
        val visitsForLocation: Option[List[String]] = visits(user).get(idToString(id))
        visitsForLocation match {
          case Some(_) => visits(user)(idToString(id)) = List()
          case None =>
        }
      case None =>
    }
  }

  override def migrate(user: User, id: T, ids: List[T]): Unit = {}
}
