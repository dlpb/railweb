package auth.api

import com.auth0.jwt.interfaces.Claim
import javax.inject.Inject
import models.auth.{User, UserDao}
import play.api.http.HeaderNames
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


case class UserRequest[A](claims: Map[String, Claim], token: String, request: Request[A], user: User) extends WrappedRequest[A](request)

class AuthorizedAction @Inject()(bodyParser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilder[UserRequest, AnyContent] {

  override def parser: BodyParser[AnyContent] = bodyParser

  override protected def executionContext: ExecutionContext = ec

  private val headerTokenRegex = """Bearer (.+?)""".r


  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
    extractBearerToken(request) map { token =>

      val service = new JWTService()
      val userApiAuthService = new UserApiAuthService(new UserDao())
      service.isValidToken(token) match {
        case Success(claims) => {
          val user = userApiAuthService.extractUserFrom(claims)
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

  private def extractBearerToken[A](request: Request[A]): Option[String] =
    request.headers.get(HeaderNames.AUTHORIZATION) collect {
      case headerTokenRegex(token) => token
    }
}