package auth

import java.util.Date

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import models.auth.roles.{MapUser, VisitUser}
import models.auth.{FileBasedUserDao, User, UserAuthService}
import org.scalatest.{FlatSpec, Matchers}

class UserApiAuthServiceTest extends FlatSpec with Matchers{

  "UserApiAuthService" should "find a user if the id is in the claim list and is valid" in {
    val dao = new FileBasedUserDao(config)
    val service = new UserAuthService(dao)

    val jwtService = new JWTService()
    val token = jwtService.createToken(User(1, "testuser", Set()), new Date())
    val claims = jwtService.isValidToken(token).get

    service.extractUserFrom(claims) should be(Some(User(1, "testuser", Set(MapUser, VisitUser))))
  }

  it should "not extract user that cannot be found" in {
    val dao = new FileBasedUserDao(config)
    val service = new UserAuthService(dao)

    val jwtService = new JWTService()
    val token = jwtService.createToken(User(2, "not found", Set()), new Date())
    val claims = jwtService.isValidToken(token).get

    service.extractUserFrom(claims) should be(None)
  }
  private def config = {
    println(getClass.getResource("").getPath)
    val path = getClass().getResource("users.json").getPath
    val config = ConfigFactory
      .empty()
      .withValue("data.user.list.root", ConfigValueFactory.fromAnyRef(
        path.substring(0, path.lastIndexOf("/"))
      ))
    config
  }


}
