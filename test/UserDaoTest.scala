import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import models.auth.roles.{MapUser, VisitUser}
import models.auth.{DaoUser, FileBasedUserDao, User, UserDao}
import models.web.forms.LoginUser
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class UserDaoTest extends FlatSpec with Matchers {

  "UserDao" should "Return true for foo login user" in {
    val foundUser = new FileBasedUserDao(config).lookupUser(LoginUser("foo", "foo"))
    foundUser should be(true)
  }

  it should "return false for non foo login user" in {
    val foundUser = new FileBasedUserDao(config).lookupUser(LoginUser("notfoo", "notfoo"))
    foundUser should be(false)
  }

  it should "return a user for id 1" in {
    val user = new FileBasedUserDao(config).findUserById(1)
    user should be(Some(User(1, "foo", Set(MapUser, VisitUser))))
  }

  it should "return a none for id 0" in {
    val user = new FileBasedUserDao(config).findUserById(0)
    user should be(None)
  }

  it should "salt and hash password" in {
    new FileBasedUserDao(config).hashAndSaltPassword("abc", "foo") should be("1cc0d51be8484747b2569e142154db5a450ff937e989c3e016fb958e54d756e5")
  }

  it should "map DaoUser to User" in {
    val user = new FileBasedUserDao(config).mapDaoUserToUser(new DaoUser(1, "foo", "foo", Set("MapUser")))
    user should be(User(1, "foo", Set(MapUser)))
  }

  it should "ignore unknown roles when mapping DAO Users and default to MapUser" in {
    val user = new FileBasedUserDao(config).mapDaoUserToUser(new DaoUser(1, "foo", "foo", Set("UNKNOWN")))
    user should be(User(1, "foo", Set(MapUser)))
  }

  private def config = {
    val path = getClass().getResource("users.json").getPath
    val config = ConfigFactory
      .empty()
      .withValue("data.user.list.root", ConfigValueFactory.fromAnyRef(
        path.substring(0, path.lastIndexOf("/"))
      ))
    config
  }
}
