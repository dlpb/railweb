package models.auth.roles

sealed trait Role
case object MapUser extends Role
case object VisitUser extends Role

object Role{
  def getRole(role: String): Role = {
    role match {
      case "VisitUser" => VisitUser
      case _ => MapUser
    }
  }
}