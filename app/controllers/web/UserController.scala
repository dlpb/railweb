package controllers.web

import javax.inject.Inject
import models.auth.UserDao
import models.web.forms.LoginUser
import models.{Global}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

class UserController @Inject()(
                                cc: MessagesControllerComponents,
                                userDao: UserDao
                              ) extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger(this.getClass)

  val form: Form[LoginUser] = Form (
    mapping(
      "username" -> nonEmptyText
        .verifying("too few chars",  s => lengthIsGreaterThanNCharacters(s, 2))
        .verifying("too many chars", s => lengthIsLessThanNCharacters(s, 200)),
      "password" -> nonEmptyText
        .verifying("too few chars",  s => lengthIsGreaterThanNCharacters(s, 2))
        .verifying("too many chars", s => lengthIsLessThanNCharacters(s, 3000)),
    )(LoginUser.apply)(LoginUser.unapply)
  )

  private val formSubmitUrl = routes.UserController.processLoginAttempt

  def showLoginForm = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.login(form, formSubmitUrl))
  }

  def processLoginAttempt = Action { implicit request: MessagesRequest[AnyContent] =>
    val errorFunction = { formWithErrors: Form[LoginUser] =>
      // form validation/binding failed...
      BadRequest(views.html.login(formWithErrors, formSubmitUrl))
    }
    val successFunction = { user: LoginUser =>
      // form validation/binding succeeded ...

      val validUser = userDao.findUserByLoginUser(user)
      validUser match {
        case Some(user) => {
          Redirect(routes.LandingPageController.showLandingPage)
          .flashing("info" -> "You are logged in.")
          .withSession(Global.SESSION_USERNAME_KEY -> user.id.toString)
        }
        case None => {
          Redirect(routes.UserController.showLoginForm)
            .flashing("error" -> "Invalid username/password.")
        }
      }
    }
    val formValidationResult: Form[LoginUser] = form.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

  private def lengthIsGreaterThanNCharacters(s: String, n: Int): Boolean = {
    if (s.length > n) true else false
  }

  private def lengthIsLessThanNCharacters(s: String, n: Int): Boolean = {
    if (s.length < n) true else false
  }

}