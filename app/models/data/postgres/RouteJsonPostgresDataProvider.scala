package models.data.postgres

import javax.inject.Inject
import models.auth.User
import models.data.{JsonDataProvider, RouteDataProvider}
import models.route.Route

class RouteJsonPostgresDataProvider @Inject() (dbProvider: PostgresDB)
  extends JsonDataProvider[Route]
  with RouteDataProvider
{
  override def writeJson(visits: Map[String, List[String]], user: User): Unit = {
    dbProvider.updateRoutesForUser(user.id, modelToString(visits))
  }

  override def readJson(user: User): Option[Map[String, List[String]]] = {
    stringToModel(dbProvider.getRoutesForUser(user.id))
  }

  override def idToString(id: Route): String = s"""from:${id.from.id}-to:${id.to.id}"""
}
