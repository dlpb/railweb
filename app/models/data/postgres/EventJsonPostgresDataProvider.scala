package models.data.postgres

import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.{Event, JsonEventDataProvider}

@Singleton
class EventJsonPostgresDataProvider @Inject()(dbProvider: PostgresDB)
  extends JsonEventDataProvider
{
  override def writeJson(events: List[Event], user: User): Unit = {
    dbProvider.updateEventsForUser(user.id, modelToString(events))
  }

  override def readJson(user: User): List[Event] = {
    stringToModel(dbProvider.getEventsForUser(user.id))
  }

}
