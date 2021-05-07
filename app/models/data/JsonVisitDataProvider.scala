package models.data

import java.time.{LocalDateTime}
import java.time.format.DateTimeFormatter

import models.auth.User
import org.json4s.JsonAST. JString
import org.json4s.{CustomSerializer, DefaultFormats}

abstract class JsonVisitDataProvider[TypeOfThingVisited, MemoryModelVisitType <: Visit[TypeOfThingVisited]]() extends DataProvider[TypeOfThingVisited, MemoryModelVisitType] {

  def writeJson(visits: List[DataModelVisit], user: User): Unit

  def readJson(user: User): List[DataModelVisit]

  override def getVisits(user: User): List[DataModelVisit] = {
    readJson(user)
  }

  override def saveVisits(visits: List[DataModelVisit], user: User) = {
    writeJson(visits, user)
  }

  override def saveVisits(json: String, user: User): Unit = writeJson(stringToModel(json), user)

  override def getVisitsAsJson(user: User): String = modelToString(readJson(user))


  override def saveVisit(id: DataModelVisit, user: User): Unit = {
    val visits: List[DataModelVisit] = readJson(user)
    val revisedVisits: List[DataModelVisit] = id :: visits
    writeJson(revisedVisits, user)
  }

  override def removeLastVisit(id: DataModelVisit, user: User): Unit = {
    val visits: List[DataModelVisit] = readJson(user)
    val revisedVisits: List[DataModelVisit] = {
      val (before, atAndAfter) = visits
        .reverse
        .span (x => x.visited != id.visited)
         before ::: atAndAfter.drop(1)
    }.reverse

    writeJson(revisedVisits, user)
  }

  override def removeAllVisits(id: DataModelVisit, user: User): Unit = {
    val visits: List[DataModelVisit] = readJson(user)
    val revisedVisits: List[DataModelVisit] =
      visits
        .filterNot(x => x.visited == id.visited)
    writeJson(revisedVisits, user)
  }

  def modelToString(visits: List[DataModelVisit]) = {
    import org.json4s.jackson.Serialization.write
    implicit val formats = DefaultFormats ++ Seq(LocalDateTimeSerializer)
    val json = write(visits)
    json
  }

  def stringToModel(contents: String): List[DataModelVisit] = {
    try {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      implicit val formats = DefaultFormats ++ Seq(LocalDateTimeSerializer)
      val allVisits = parse(contents).extract[List[DataModelVisit]]
      allVisits
    }
    catch {
      case e: Exception => e.printStackTrace()
        List.empty[DataModelVisit]
    }
  }

//  override def migrate(user: User, id: MemoryModelVisitType, ids: List[MemoryModelVisitType]): Unit = {
//    val oldKey = idToString(id)
//
//    readJson(user) foreach {
//      data =>
//        val entryForOldKey: Option[List[String]] = data.get(oldKey)
//        val entriesForNewKeys: Map[String, List[String]] = entryForOldKey match {
//          case Some(visits) =>
//             ids.map( idToString(_) -> visits ).toMap
//          case _ => Map.empty
//        }
//        writeJson(data - oldKey ++ entriesForNewKeys, user)
//    }
//  }
}


