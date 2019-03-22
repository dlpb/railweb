package auth.api

import com.auth0.jwt.interfaces.Claim
import models.auth.{User, UserDao}
import models.auth.roles.Role

class UserApiAuthService(userDao: UserDao) {
  def extractUserFrom(claims: Map[String, Claim]): Option[User] = {
    claims.get("userId") map { _.asLong } flatMap { id => userDao.findUserById(id)}
  }

  def getRolesFor(user: User): Set[Role] ={
    user.roles
  }
}
