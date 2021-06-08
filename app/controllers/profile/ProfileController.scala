package controllers.profile

import java.util.Date

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject._
import models.auth.UserDao
import models.web.forms.ChangePassword
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc._

@Singleton
class ProfileController @Inject()(
                                   userDao: UserDao,
                                   jwtService: JWTService,
                                   cc: ControllerComponents,
                                   authenticatedUserAction: AuthorizedWebAction,
                                   authorizedAction: AuthorizedAction
                                           ) extends AbstractController(cc) {

  val form: Form[ChangePassword] = Form(
    mapping(
      "oldPassword" -> nonEmptyText,
      "newPassword" -> nonEmptyText,
      "confirmPassword" -> nonEmptyText,
    )(ChangePassword.apply)(ChangePassword.unapply)
  )

  def index = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    val token = jwtService.createToken(request.user, new Date())
    Ok(views.html.profile.index(token, request.user, form, List()))
  }


}
