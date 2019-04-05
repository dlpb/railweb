package models.auth

import com.typesafe.config.Config
import javax.inject.Inject
import models.data.postgres.PostgresDB

@javax.inject.Singleton
class PostgresBasedUserDao @Inject()(config: Config, dbProvider: PostgresDB) extends UserDao(config) {

    override def getUsers: Set[DaoUser] = {
      dbProvider.getUsers().toSet
    }

  override def updateUser(user: DaoUser): Unit = {
    dbProvider.updateUser(user.copy(password = encryptPassword(user.password)))
    users = users.filterNot(u => u.id.equals(user.id)) + user
  }

  override def createUser(username: String, password: String, roles: Set[String]): Unit = {
    val user = makeDaoUserFromRawData(username, password, roles)
    dbProvider.createUser(user)
    users = users + user
  }

  override def deleteUser(user: DaoUser): Unit = {
    dbProvider.deleteUserById(user.id)
    users = users - user
  }
}