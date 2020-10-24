package controllers.profile.visit

import auth.JWTService
import auth.api.AuthorizedAction
import auth.web.{AuthorizedWebAction, WebUserContext}
import javax.inject.{Inject, Singleton}
import models.auth.UserDao
import services.visit.route.RouteVisitService
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}

@Singleton
class VisitsController @Inject()(
                                  userDao: UserDao,
                                  jwtService: JWTService,
                                  cc: ControllerComponents,
                                  routesService: RouteVisitService,
                                  authenticatedUserAction: AuthorizedWebAction,
                                  authorizedAction: AuthorizedAction
                                ) extends AbstractController(cc) {

  def index = authenticatedUserAction { implicit request: WebUserContext[AnyContent] =>
    Ok(views.html.visits.index(request.user))

  }

}

