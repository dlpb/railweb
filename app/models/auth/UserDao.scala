package models.auth

import javax.inject.Inject
import models.auth.roles.{MapUser, VisitUser}
import models.web.forms.LoginUser

@javax.inject.Singleton
class UserDao @Inject()() {

  def lookupUser(u: LoginUser): Boolean = {
    //TODO query your database here
    if (u.username == "foo" && u.password == "foo") true else false
  }

  def findUserByLoginUser(user: LoginUser) ={
    if(user.username.equals("foo") && user.password.equals("foo")) Some(User(1L, "foo", Set(MapUser, VisitUser)))
    else None
  }

  def findUserById(id: Long): Option[User] = {
    if(id.equals(1L)) Some(User(1L, "foo", Set(MapUser, VisitUser)))
    else None
  }

}
