package models.data.postgres

import javax.inject.Inject
import models.auth.User
import models.data.{JsonDataProvider, LocationDataProvider}
import models.location.Location

class LocationJsonPostgresDataProvider @Inject() (dbProvider: PostgresDB)
  extends JsonDataProvider[Location]
  with LocationDataProvider
{
  override def writeJson(visits: Map[String, List[String]], user: User): Unit = {
    dbProvider.updateLocationsForUser(user.id, modelToString(visits))
  }

  override def readJson(user: User): Option[Map[String, List[String]]] = {
    stringToModel(dbProvider.getLocationsForUser(user.id))
  }

  override def idToString(id: Location): String = id.id
}
