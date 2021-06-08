package auth

import java.time.{LocalDateTime, ZoneId, ZoneOffset}
import java.util
import java.util.{Calendar, Date}

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.Claim
import models.auth.User

import scala.util.{Failure, Success, Try}


class JWTService {

  private val algo = Algorithm.HMAC256("SECRET")
  private val subject = "rail.dlpb.uk/api"
  private val issuer = "rail.dlpb.uk"

  def createToken(user: User, createDate: Date): String = {
    val id: java.lang.Long = user.id
    val expires = Date.from(createDate.toInstant.atZone(ZoneId.systemDefault()).toLocalDateTime.plusDays(1).toInstant(ZoneOffset.UTC))
    val token = JWT.create()
      .withIssuedAt(createDate)
      .withKeyId("JWT Key")
      .withSubject(subject)
      .withAudience(subject)
      .withNotBefore(createDate)
      .withExpiresAt(expires)
      .withClaim("userId", id)
      .withClaim("username", user.username)
      .withIssuer(issuer).sign(algo)
    token
  }

  def isValidToken(token: String): Try[Map[String, Claim]] = {
    try {
      val verifier = JWT.require(algo).withIssuer(issuer).build()
      val jwt = verifier.verify(token)
      Success(toScalaMap(jwt.getClaims))
    }
    catch {
      case e: JWTVerificationException  => Failure(e)
    }
  }

  private def toScalaMap(claims: util.Map[String, Claim]): Map[String, Claim] = {
    import scala.jdk.CollectionConverters._
    claims.asScala.toMap
  }
//  def decodePayload(token: String): Option[String] = {
//    val verifier = JWT.require(algo).withIssuer("rail.dlpb.uk").build()
//    val jwt = verifier.verify(token)
//    import scala.language.postfixOps
//    import scala.collection.JavaConverters._
//    val claims: mutable.Map[String, Claim] = jwt.getClaims asScala
//
//    Some(claims map {
//      c =>
//        val claim = c._2
//        claim.asString()
//    } mkString(","))
//
//  }
}
