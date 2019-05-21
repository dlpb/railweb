package models.data.file

import java.nio.file.{Files, Path, Paths}

import com.typesafe.config.Config
import models.auth.User
import models.data.JsonDataProvider

import scala.io.Source

abstract class JsonFileBasedDataProvider[T](config: Config) extends JsonDataProvider[T]() {

  def dataPath: String

  def root: String = config.getString("data.user.root")

  def writeJson(visits: Map[String, List[String]], user: User): Unit = {
    val path = Paths.get(getPathToFile(user))
    if(fileExists(path)){
      val json: String = modelToString(visits)
      Files.write(path, json.getBytes())
    }
    else {
      Files.createDirectories(Paths.get(getPathToDirectory(user)))
      Files.createFile(path)
      writeJson(visits, user)
    }
  }

  def readJson(user: User): Option[Map[String, List[String]]] = {
    val path = Paths.get(getPathToFile(user))
    if(fileExists(path)){
      val contents = Source.fromFile(path.toFile).mkString
      stringToModel(contents)
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
