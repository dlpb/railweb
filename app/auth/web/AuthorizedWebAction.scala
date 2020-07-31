package auth.web

import auth.JWTService
import controllers.routes
import javax.inject.Inject
import models.auth.{User, UserAuthService, UserDao}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

case class WebUserContext[A](user: User, request: Request[A]) extends WrappedRequest[A](request)

class AuthorizedWebAction @Inject()(
                                     userDao: UserDao,
                                     bodyParser: BodyParsers.Default,
                                     jwtService: JWTService,
                                     userAuthService: UserAuthService
                                   )(implicit ec: ExecutionContext)
  extends ActionBuilder[WebUserContext, AnyContent] {

  override def parser: BodyParser[AnyContent] = bodyParser
  override protected def executionContext: ExecutionContext = ec

  private val logger = play.api.Logger(this.getClass)

  override def invokeBlock[A](request: Request[A], block: WebUserContext[A] => Future[Result]) = {
    logger.info("ENTERED AuthenticatedUserAction::invokeBlock ...")
    val maybeUsername = request.session.get(models.Global.SESSION_USERNAME_KEY)
    val maybeToken = jwtService.isValidToken(request.session.get(models.Global.SESSION_USER_TOKEN).getOrElse(""))
    val maybeUser: Option[User] = maybeUsername flatMap { id => userDao.findUserById(id.toLong)}
    maybeUser match {
      case None => {
        Future.successful(Redirect(controllers.login.routes.UserController.showLoginForm())
          .flashing("error" -> "Invalid username/password."))
      }
      case Some(user: User) => {
        maybeToken match {
          case Success(claims: Map[String, com.auth0.jwt.interfaces.Claim]) =>
            val tokenUser = userAuthService.extractUserFrom(claims)
            tokenUser match {
              case Some(u) if user.id.equals(u.id) =>
                val res: Future[Result] = block(WebUserContext(user, request))
                res
              case None =>
                Future.successful(Redirect(controllers.login.routes.UserController.showLoginForm())
                  .flashing("error" -> "User logged out. Please log in again.").withNewSession)

            }
          case _ =>
            Future.successful(Redirect(controllers.login.routes.UserController.showLoginForm())
              .flashing("error" -> "User logged out. Please log in again.").withNewSession)
        }
      }
    }
  }
}