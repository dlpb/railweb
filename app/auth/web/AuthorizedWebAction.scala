package auth.web

import controllers.routes
import javax.inject.Inject
import models.auth.{User, UserDao}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class WebUserContext[A](user: User, request: Request[A]) extends WrappedRequest[A](request)

class AuthorizedWebAction @Inject()(parser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilder[WebUserContext, AnyContent] {

  override def parser: BodyParser[AnyContent] = parser
  override protected def executionContext: ExecutionContext = ec

  private val logger = play.api.Logger(this.getClass)

  private val userDao: UserDao = new UserDao()

  override def invokeBlock[A](request: Request[A], block: WebUserContext[A] => Future[Result]) = {
    logger.info("ENTERED AuthenticatedUserAction::invokeBlock ...")
    val maybeUsername = request.session.get(models.Global.SESSION_USERNAME_KEY)
    val maybeUser: Option[User] = maybeUsername flatMap { id => userDao.findUserById(id.toLong)}
    maybeUser match {
      case None => {
        Future.successful(Redirect(routes.UserController.showLoginForm)
          .flashing("error" -> "Invalid username/password."))
      }
      case Some(user: User) => {
        val res: Future[Result] = block(WebUserContext(user, request))
        res
      }
    }
  }
}