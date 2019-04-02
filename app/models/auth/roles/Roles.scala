package models.auth.roles

sealed trait Role
case object MapUser extends Role
case object VisitUser extends Role