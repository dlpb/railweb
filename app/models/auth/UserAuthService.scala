package models.auth

import com.auth0.jwt.interfaces.Claim
import javax.inject.Inject
import models.auth.roles.Role

@Singleton
class UserAuthService @Inject() (userDao: UserDao) {
  def extractUserFrom(claims: Map[String, Claim]): Option[User] = {
    val maybeUser = claims.get("userId") map { _.asLong } flatMap { id => userDao.findUserById(id)}
    val username = claims.get("username") map { _.asString() } getOrElse ""
    maybeUser match {
      case Some(user) if user.username.equals(username) =>
        Some(user)
      case _ =>
        None
    }
  }

  def getRolesFor(user: User): Set[Role] ={
    user.roles
  }
}
