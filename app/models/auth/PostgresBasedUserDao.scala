package models.auth

import com.typesafe.config.Config
import javax.inject.Inject
import models.data.postgres.PostgresDB

@javax.inject.Singleton
class PostgresBasedUserDao @Inject()(config: Config, dbProvider: PostgresDB) extends UserDao(config) {

    override def getUsers: Set[DaoUser] = {
      dbProvider.getUsers().toSet
    }

  override def updateUser(user: DaoUser): Unit = {}
}