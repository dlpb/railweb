package models.data

import models.auth.User
import org.json4s.DefaultFormats

abstract class JsonEventDataProvider() extends EventDataProvider {

  def writeJson(visits: List[Event], user: User): Unit

  def readJson(user: User): List[Event]

  override def getEventsAsJson(user: User): String = modelToString(readJson(user))

  override def saveEventsAsJson(events: String, user: User): Unit = writeJson(stringToModel(events), user)

  override def getEvents(user: User): List[Event] = {
    readJson(user)
  }

  override def saveEvents(events: List[Event], user: User) = {
    writeJson(events, user)
  }

  override def saveEvent(event: Event, user: User): Unit = {
    val events: List[Event] = readJson(user)
    val revisedEvents: List[Event] = event :: events
    writeJson(revisedEvents, user)
  }

  override def removeLastEvent(events: List[Event], user: User): Unit = {
    val visits: List[Event] = readJson(user)
    val revisedVisits: List[Event] = visits.dropRight(1)

    writeJson(revisedVisits, user)
  }

  override def removeAllEvents(events: List[Event], user: User): Unit = {
    val revisedVisits: List[Event] = List.empty[Event]
    writeJson(revisedVisits, user)
  }

  def modelToString(events: List[Event]): String = {
    import org.json4s.jackson.Serialization.write
    implicit val formats = DefaultFormats ++ Seq(LocalDateTimeSerializer)
    val json = write(events)
    json
  }

  def stringToModel(contents: String): List[Event] = {
    try {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      implicit val formats = DefaultFormats ++ Seq(LocalDateTimeSerializer)
      val allVisits = parse(contents).extract[List[Event]]
      allVisits
    }
    catch {
      case e: Exception => List.empty[Event]
    }
  }
}
