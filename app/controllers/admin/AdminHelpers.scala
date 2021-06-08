package controllers.admin

import auth.api.UserRequest
import models.auth.UserDao
import play.api.mvc.{AnyContent, Result}

object AdminHelpers {
  def ensureValidConfirmation(userDao: UserDao,
                              request: UserRequest[AnyContent],
                              data: Option[Map[String, Seq[String]]],
                              view: List[String] => Result,
                              fn: Option[Map[String, Seq[String]]] => Result) = {
    data.get("confirmation").headOption match {
      case Some(confirmation) =>
        userDao.getDaoUser(request.user) match {
          case Some(adminUser) =>
            val encryptedConfirmation = userDao.encryptPassword(confirmation)
            if (encryptedConfirmation.equals(adminUser.password)) {
              fn(data)
            }
            else {
              view(List("Invalid confirmation"))
            }
          case None =>
            view(List("Error finding admin user"))
        }
      case None =>
        view(List("Please enter confirmation"))
    }
  }
}
