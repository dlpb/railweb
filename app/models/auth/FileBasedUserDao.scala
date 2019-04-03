package models.auth

import com.typesafe.config.Config
import javax.inject.Inject
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.io.Source

@javax.inject.Singleton
class FileBasedUserDao @Inject()(config: Config) extends UserDao(config) {

  def makeUsers(data: String): Set[DaoUser] = {
    implicit val formats = DefaultFormats
    parse(data).extract[Set[DaoUser]]
  }

  def readUsersFromFile(): String = {
    Source.fromFile(dataRoot + "/users.json").mkString
  }

  override def getUsers: Set[DaoUser] = makeUsers(readUsersFromFile())

  override def updateUser(user: DaoUser): Unit = ???

  override def createUser(username: String, password: String, roles: Set[String]): Unit = {}

  override def deleteUser(user: DaoUser): Unit = {}
}