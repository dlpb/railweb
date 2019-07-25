package models.auth

import java.math.BigInteger
import java.security.MessageDigest

import com.typesafe.config.Config
import models.auth.roles.Role
import models.web.forms.LoginUser

import scala.io.Source

abstract class UserDao(config: Config) extends UserProvider {


  def dataRoot = config.getString("data.user.list.root")

  private[auth] var users: Set[DaoUser] = getUsers
  private val salt: String = readSalt

  def lookupUser(u: LoginUser): Boolean = {
    val matchingUsers = users.filter(user => u.username.equals(user.username) && hashAndSaltPassword(salt, u.password).equals(user.password))
    matchingUsers.size == 1
  }
  def getDaoUser(user: User): Option[DaoUser] = users.find(u => user.id.equals(u.id) && user.username.equals(u.username))

  def usernameInUse(username: String): Boolean = users.exists(user => user.username.equals(username))

  def findUserByLoginUser(u: LoginUser): Option[User] = {
    users.find({user =>
      println(s"DEBUG: ${user}, ${hashAndSaltPassword(salt, u.password)}")
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

  def makeDaoUserFromRawData(username: String, password: String, roles: Set[String]) = {
    DaoUser(0, username, encryptPassword(password), roles)
  }

  def encryptPassword(password: String) = hashAndSaltPassword(salt, password)

  def readSalt = Source.fromFile(dataRoot + "/salt.txt").mkString

  def hashAndSaltPassword(salt: String, password: String) = {
    val toHash = salt + password
    String.format("%032x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(toHash.getBytes("UTF-8"))))
  }

}

case class DaoUser(id: Long, username: String, password: String, roles: Set[String])