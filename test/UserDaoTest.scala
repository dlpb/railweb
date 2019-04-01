import models.auth.roles.MapUser
import models.auth.{User, UserDao}
import models.web.forms.LoginUser
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class UserDaoTest extends FlatSpec with Matchers {

  "UserDao" should "Return true for foo login user" in {
    val foundUser = new UserDao().lookupUser(LoginUser("foo", "foo"))
    foundUser should be(true)
  }

  it should "return false for non foo login user" in {
    val foundUser = new UserDao().lookupUser(LoginUser("notfoo", "notfoo"))
    foundUser should be(false)
  }

  it should "return a user for id 1" in {
    val user = new UserDao().findUserById(1)
    user should be(Some(User(1, "foo", Set(MapUser))))
  }

  it should "return a none for id 0" in {
    val user = new UserDao().findUserById(0)
    user should be(None)
  }

  ignore should "test" in {
      val inputStream = getClass()
        .getClassLoader()
        .getResourceAsStream("data/static/locations.json")
      println("input stream loaded")
      val jsonString = Source.fromInputStream(inputStream).mkString
      println("json loaded")
  }

}
