package models.data.postgres

import javax.inject.{Inject, Singleton}
import models.auth.User
import models.data.{DataModelVisit, JsonVisitDataProvider, LocationDataProvider, LocationVisit}
import models.location.Location
import services.location.LocationService

@Singleton
class LocationJsonPostgresVisitDataProvider @Inject()(dbProvider: PostgresDB,
                                                      ls: LocationService)
  extends JsonVisitDataProvider[Location, LocationVisit]
  with LocationDataProvider
{
  override def writeJson(visits: List[DataModelVisit], user: User): Unit = {
    dbProvider.updateLocationsForUser(user.id, modelToString(visits))
  }

  override def readJson(user: User): List[DataModelVisit] = {
    stringToModel(dbProvider.getLocationsForUser(user.id))
  }

  override def locationService: LocationService = ls
}
