package controllers.about

import auth.JWTService
import javax.inject.Inject
import models.auth.UserDao
import play.api.mvc._

class AboutController @Inject()(
                                 cc: MessagesControllerComponents,
                                 userDao: UserDao,
                                 jwtService: JWTService
                               ) extends MessagesAbstractController(cc) {

  def about = Action { implicit request =>
    Ok(views.html.about.index())
  }


}