package auth

import java.util.Date

import auth.api.{JWTService, UserApiAuthService}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import models.auth.{FileBasedUserDao, User}
import org.scalatest.{FlatSpec, Matchers}

class UserApiAuthServiceTest extends FlatSpec with Matchers{

  "UserApiAuthService" should "find a user if the id is in the claim list and is valid" in {
    val dao = new FileBasedUserDao(config)
    val service = new UserApiAuthService(dao)

    val jwtService = new JWTService()
    val token = jwtService.createToken(User(1, "foo", Set()), new Date())
    val claims = jwtService.isValidToken(token).get

    service.extractUserFrom(claims) should be(Some(User(1, "foo", Set())))
  }

  it should "not extract user that cannot be found" in {
    val dao = new FileBasedUserDao(config)
    val service = new UserApiAuthService(dao)

    val jwtService = new JWTService()
    val token = jwtService.createToken(User(2, "not found", Set()), new Date())
    val claims = jwtService.isValidToken(token).get

    service.extractUserFrom(claims) should be(None)
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
