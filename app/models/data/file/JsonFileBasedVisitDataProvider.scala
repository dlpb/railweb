package models.data.file

import java.nio.file.{Files, Path, Paths}

import com.typesafe.config.Config
import models.auth.User
import models.data.{DataModelVisit, JsonVisitDataProvider, Visit}

import scala.io.Source

abstract class JsonFileBasedVisitDataProvider[TypeOfThingVisited, MemoryModelVisitType <: Visit[TypeOfThingVisited]](config: Config) extends JsonVisitDataProvider[TypeOfThingVisited, MemoryModelVisitType]() {

  def dataPath: String

  def root: String = config.getString("data.user.root")

  def writeJson(visits: List[DataModelVisit], user: User): Unit = {
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

  def readJson(user: User): List[DataModelVisit] = {
    val path = Paths.get(getPathToFile(user))
    if(fileExists(path)){
      val contents = Source.fromFile(path.toFile).mkString
      stringToModel(contents)
    }
    else {
      List.empty[DataModelVisit]
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
