package models.data.postgres

import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.{JsonDataProvider, RouteDataProvider}
import models.route.Route

@Singleton
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

object RouteDataIdConverter {
  def stringToRouteIds(id: String): (String, String) = {
    val fromTo = id.split("-")
    val from = fromTo(0).split(":")(1)
    val to = fromTo(1).split(":")(1)
    (from, to)
  }
}