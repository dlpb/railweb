package models.data.postgres

import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.{DataModelVisit, JsonVisitDataProvider, RouteDataProvider, RouteVisit}
import models.route.Route
import services.route.RouteService

@Singleton
class RouteJsonPostgresVisitDataProvider @Inject()(dbProvider: PostgresDB, rs: RouteService)
  extends JsonVisitDataProvider[Route, RouteVisit]
  with RouteDataProvider
{
  override def writeJson(visits: List[DataModelVisit], user: User): Unit = {
    dbProvider.updateRoutesForUser(user.id, modelToString(visits))
  }

  override def readJson(user: User): List[DataModelVisit] = {
    stringToModel(dbProvider.getRoutesForUser(user.id))
  }

  override def routeService: RouteService = rs
}
