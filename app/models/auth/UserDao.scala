package models.auth

import java.math.BigInteger
import java.security.MessageDigest

import com.typesafe.config.Config
import javax.inject.Inject
import models.auth.roles.{MapUser, Role, VisitUser}
import models.web.forms.LoginUser
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.io.Source

@javax.inject.Singleton
class UserDao @Inject()(config: Config) {

  def dataRoot = config.getString("data.user.list.root")

  private val users: Set[DaoUser] = makeUsers(readUsersFromFile)
  private val salt: String = readSalt

  def lookupUser(u: LoginUser): Boolean = {
    val matchingUsers = users.filter(user => u.username.equals(user.username) && hashAndSaltPassword(salt, u.password).equals(user.password))
    matchingUsers.size == 1
  }

  def findUserByLoginUser(u: LoginUser): Option[User] = {
    users.find({user =>
      u.username.equals(user.username) && hashAndSaltPassword(salt, u.password).equals(user.password)}
    ) map mapDaoUserToUser

  }

  def mapDaoUserToUser(daoUser: DaoUser): User = {
    val roles = daoUser.roles.map({ role =>
      Role.getRole(role)
    })
    User(daoUser.id, daoUser.username, roles)
  }

  def findUserById(id: Long): Option[User] = {
    users.find(user => {
      val userId: Long = user.id
      userId.equals(id)
    }) map mapDaoUserToUser
  }

  def makeUsers(data: String): Set[DaoUser] = {
    implicit val formats = DefaultFormats
    parse(data).extract[Set[DaoUser]]
  }

  def readUsersFromFile(): String = {
    Source.fromFile(dataRoot + "/users.json").mkString
  }

  def readSalt = Source.fromFile(dataRoot + "/salt.txt").mkString

  def hashAndSaltPassword(salt: String, password: String) = {
    val toHash = salt + password
    String.format("%032x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(toHash.getBytes("UTF-8"))))
  }

}

case class DaoUser(id: Long, username: String, password: String, roles: Set[String])