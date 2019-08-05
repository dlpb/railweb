package auth

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import models.auth.User
import models.auth.roles.MapUser
import org.scalatest.{FlatSpec, Matchers}

class JWTServiceTest extends FlatSpec with Matchers{
  "JWTService" should "mint a token for a user created at a specific date" in {
    val service = new JWTService()
    val date: Date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2000-01-01 00:00:00")
    val token = service.createToken(User(1, "foo", Set()), date)

    token should be("eyJraWQiOiJKV1QgS2V5IiwidHlwIjoiSldUIiwiYWxnIjoiSFMyNTYifQ.eyJzdWIiOiJyYWlsLmRscGIudWsvYXBpIiwiYXVkIjoicmFpbC5kbHBiLnVrL2FwaSIsIm5iZiI6OTQ2Njg0ODAwLCJpc3MiOiJyYWlsLmRscGIudWsiLCJleHAiOjk0Njc3MTIwMCwiaWF0Ijo5NDY2ODQ4MDAsInVzZXJJZCI6MSwidXNlcm5hbWUiOiJmb28ifQ.Ly9mn-iosV0ImWwglJwjrEFQrkZTV6dzRh_siKf-TGM")
  }

  it should "mint a token with the user id in it" in {
    val service = new JWTService()
    val date: Date =new Date(System.currentTimeMillis())
    val token = service.createToken(User(1, "foo", Set(MapUser)), date)
    service.isValidToken(token).isSuccess should be(true)
    service.isValidToken(token).get("userId").asLong should be(1)
  }

  it should "Not validate an invalid token" in {
    val service = new JWTService()
    service.isValidToken("NOT VALID").isFailure should be(true)
  }
}
