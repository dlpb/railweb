package models.data
import java.nio.file.{Files, Path, Paths}

import com.typesafe.config.Config
import models.auth.User
import org.json4s.DefaultFormats

import scala.io.Source

abstract class JsonFileBasedDataProvider[T](config: Config) extends DataProvider[T] {

  def dataPath: String

  def root: String = config.getString("data.user.root")

  override def getVisits(user: User): Option[Map[String, List[String]]] = {
    val path = Paths.get(getPathToFile(user))
    readJsonFile(path)
  }

  override def saveVisit(id: T, user: User): Unit = {
    val path = Paths.get(getPathToFile(user))

    val visits: Option[Map[String, List[String]]] = readJsonFile(path)
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

    writeJsonFile(path, revisedVisits, user)
  }

  override def removeLastVisit(id: T, user: User): Unit = {
    val path = Paths.get(getPathToFile(user))

    val visits: Option[Map[String, List[String]]] = readJsonFile(path)
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

    writeJsonFile(path, revisedVisits, user)
  }

  override def removeAllVisits(id: T, user: User): Unit = {
    val path = Paths.get(getPathToFile(user))

    val visits: Option[Map[String, List[String]]] = readJsonFile(path)
    val revisedVisits: Map[String, List[String]] = visits match {
      case Some(data) =>
        val visitsToLocation: Option[List[String]] = data.get(idToString(id))
        visitsToLocation match {
          case Some(_) => data + (idToString(id) -> List.empty[String])
          case None => data
        }
      case None => Map()
    }

    writeJsonFile(path, revisedVisits, user)
  }

  def writeJsonFile(path: Path, visits: Map[String, List[String]], user: User): Unit = {
    if(fileExists(path)){
      import org.json4s.jackson.Serialization.write
      implicit val formats = DefaultFormats
      val json = write(visits)
      Files.write(path, json.getBytes())
    }
    else {
      Files.createDirectories(Paths.get(getPathToDirectory(user)))
      Files.createFile(path)
      writeJsonFile(path, visits, user)
    }
  }

  def readJsonFile(path: Path): Option[Map[String, List[String]]] = {
    if(fileExists(path)){
      val contents = Source.fromFile(path.toFile).mkString
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      implicit val formats = DefaultFormats
      val allVisits = parse(contents).extract[Map[String, List[String]]]
      Some(allVisits)
    }
    else {
      None
    }
  }

  def fileExists(path: Path): Boolean = Files.exists(path)

  private def getPathToFile(user: User) = {
    s"${getPathToDirectory(user)}/visits.json"
  }

  private def getPathToDirectory(user: User) = {
    s"$root/${user.id}/$dataPath/"
  }
}
