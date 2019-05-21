package models.data

import org.json4s.DefaultFormats

object VisitJsonUtils {
  def toJson(data: Option[Map[String, List[String]]]): String = {
    import org.json4s.native.Serialization.writePretty
    implicit val formats = DefaultFormats
    val json = writePretty(data)
    json
  }

  def fromJson(data: String): Option[Map[String, List[String]]] = {
    try {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      implicit val formats = DefaultFormats
      val allVisits = parse(data).extract[Map[String, List[String]]]
      Some(allVisits)
    }
    catch {
      case e: Exception => None
    }
  }
}
