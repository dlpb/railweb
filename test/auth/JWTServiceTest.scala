package auth

import java.text.SimpleDateFormat
import java.util.Date

import auth.api.JWTService
import models.auth.User
import org.scalatest.{FlatSpec, Matchers}

class JWTServiceTest extends FlatSpec with Matchers{
  "JWTService" should "mint a token for a user created at a specific date" in {
    val service = new JWTService()
    val date: Date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2000-01-01 00:00:00")
    val token = service.createToken(User(1, "foo", Set()), date)

    token should be("eyJraWQiOiJKV1QgS2V5IiwidHlwIjoiSldUIiwiYWxnIjoiSFMyNTYifQ.eyJzdWIiOiJyYWlsLmRscGIudWsvYXBpIiwiYXVkIjoicmFpbC5kbHBiLnVrL2FwaSIsIm5iZiI6OTQ2Njg0ODAwLCJpc3MiOiJyYWlsLmRscGIudWsiLCJpYXQiOjk0NjY4NDgwMCwidXNlcklkIjoxfQ.tKw-6L_lBTIX-ipa-QVvV0W73Ny-c-SAOIEvT0Ul3ic")
  }

  it should "mint a token with the user id in it" in {
    val service = new JWTService()
    val date: Date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2000-01-01 00:00:00")
    val token = service.createToken(User(1, "foo", Set()), date)
    service.isValidToken(token).isSuccess should be(true)
    service.isValidToken(token).get("userId").asLong should be(1)
  }

  it should "Not validate an invalid token" in {
    val service = new JWTService()
    service.isValidToken("NOT VALID").isFailure should be(true)
  }
}
