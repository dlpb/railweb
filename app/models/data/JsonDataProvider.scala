package models.data

import models.auth.User
import org.json4s.DefaultFormats

abstract class JsonDataProvider[T]() extends DataProvider[T] {

  def writeJson(visits: Map[String, List[String]], user: User): Unit

  def readJson(user: User): Option[Map[String, List[String]]]

  override def getVisits(user: User): Option[Map[String, List[String]]] = {
    readJson(user)
  }

  override def saveVisits(visits: Option[Map[String, List[String]]], user: User) = {
    visits foreach { v =>
      writeJson(removeEmptyVisits(v), user)
    }
  }

  override def saveVisit(id: T, user: User): Unit = {
    val visits: Option[Map[String, List[String]]] = readJson(user)
    val revisedVisits: Map[String, List[String]] = visits match {
      case Some(data) => data.get(idToString(id)) match {
        case Some(vl) =>
          val additionalVisit: List[String] = timestamp() :: vl
          data + (idToString(id) -> additionalVisit)
        case None => data + (idToString(id) -> List(timestamp()))
      }
      case None =>
        Map(idToString(id) -> List(timestamp()))
    }
    writeJson(removeEmptyVisits(revisedVisits), user)
  }

  override def removeLastVisit(id: T, user: User): Unit = {
    val visits: Option[Map[String, List[String]]] = readJson(user)
    val revisedVisits: Map[String, List[String]] = visits match {
      case Some(data) =>
        val visitsToLocation: Option[List[String]] = data.get(idToString(id))
        visitsToLocation match {
          case Some(vl) =>
            vl match {
              case _ :: tail => data + (idToString(id) -> tail)
              case _ => data + (idToString(id) -> List.empty[String])
            }
          case None => data
        }
      case None => Map()
    }
    writeJson(removeEmptyVisits(revisedVisits), user)
  }

  override def removeAllVisits(id: T, user: User): Unit = {
    val visits: Option[Map[String, List[String]]] = readJson(user)
    val revisedVisits: Map[String, List[String]] = visits match {
      case Some(data) =>
        val visitsToLocation: Option[List[String]] = data.get(idToString(id))
        visitsToLocation match {
          case Some(_) => data + (idToString(id) -> List.empty[String])
          case None => data
        }
      case None => Map()
    }
    writeJson(removeEmptyVisits(revisedVisits), user)
  }

  def removeEmptyVisits(visits: Map[String, List[String]]) = visits.filter(_._2.nonEmpty)

  def modelToString(visits: Map[String, List[String]]) = {
    import org.json4s.jackson.Serialization.write
    implicit val formats = DefaultFormats
    val json = write(visits)
    json
  }

  def stringToModel(contents: String) = {
    try {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      implicit val formats = DefaultFormats
      val allVisits = parse(contents).extract[Map[String, List[String]]]
      Some(allVisits)
    }
    catch {
      case e: Exception => None
    }
  }

  override def migrate(user: User, id: T, ids: List[T]): Unit = {
    val oldKey = idToString(id)

    readJson(user) foreach {
      data =>
        val entryForOldKey: Option[List[String]] = data.get(oldKey)
        val entriesForNewKeys: Map[String, List[String]] = entryForOldKey match {
          case Some(visits) =>
             ids.map( idToString(_) -> visits ).toMap
          case _ => Map.empty
        }
        writeJson(data - oldKey ++ entriesForNewKeys, user)
    }
  }
}
