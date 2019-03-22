package models.auth

import models.auth.roles.Role

case class User(
                 id: Long,
                 username: String,
                 roles: Set[Role]
               )
