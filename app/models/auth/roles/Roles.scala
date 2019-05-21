package models.auth.roles

sealed trait Role
case object MapUser extends Role
case object VisitUser extends Role
case object AdminUser extends Role

object Role{
  def getRole(role: String): Role = {
    role match {
      case "VisitUser" => VisitUser
      case "AdminUser" => AdminUser
      case _ => MapUser
    }
  }
}