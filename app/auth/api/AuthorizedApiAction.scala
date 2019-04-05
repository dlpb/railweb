package auth.api

import auth.JWTService
import com.auth0.jwt.interfaces.Claim
import javax.inject.Inject
import models.auth.{User, UserAuthService, UserDao}
import play.api.http.HeaderNames
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


case class UserRequest[A](claims: Map[String, Claim], token: String, request: Request[A], user: User) extends WrappedRequest[A](request)

class AuthorizedAction @Inject()(
                                  userDao: UserDao,
                                  bodyParser: BodyParsers.Default,
                                  userAuthService: UserAuthService
                                )(implicit ec: ExecutionContext)
  extends ActionBuilder[UserRequest, AnyContent] {

  override def parser: BodyParser[AnyContent] = bodyParser

  override protected def executionContext: ExecutionContext = ec

  private val headerTokenRegex = """Bearer (.+?)""".r


  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
    extractBearerToken(request) map { token =>

      val service = new JWTService()
      service.isValidToken(token) match {
        case Success(claims) => {
          val user = userAuthService.extractUserFrom(claims)
          user match {
            case Some(user) => block(UserRequest(claims, token, request, user))
            case None => Future.successful(Results.Unauthorized("User error"))
          }
        }
        case Failure(t) => Future.successful(Results.Unauthorized(t.getMessage))
      }
    } getOrElse {
      Future.successful(Results.Unauthorized)
    }
  }

  private def extractBearerToken[A](request: Request[A]): Option[String] = {
    val fromHeader = request.headers.get(HeaderNames.AUTHORIZATION) collect {
      case headerTokenRegex(token) => token
    }
    fromHeader match {
      case None =>
        val data = request.body.asInstanceOf[AnyContentAsFormUrlEncoded].data
        data.get("Authorization").map {_.head}
      case x => x
    }
  }
}