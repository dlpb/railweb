import com.google.inject.AbstractModule
import com.typesafe.config.Config
import models.auth.{FileBasedUserDao, PostgresBasedUserDao, UserDao}
import models.data._
import models.data.postgres.{EventJsonPostgresDataProvider, LocationJsonPostgresVisitDataProvider, RouteJsonPostgresVisitDataProvider}
import models.plan.timetable.reader.{Reader, WebZipInputStream}

class Module extends AbstractModule {
  override def configure() = {

    bind(classOf[LocationDataProvider])
      .to(classOf[LocationJsonPostgresVisitDataProvider])

    bind(classOf[RouteDataProvider])
      .to(classOf[RouteJsonPostgresVisitDataProvider])

    bind(classOf[EventDataProvider])
      .to(classOf[EventJsonPostgresDataProvider])

//    bind(classOf[LocationDataProvider])
//      .to(classOf[LocationJsonFileVisitDataProvider])
//
//    bind(classOf[RouteDataProvider])
//      .to(classOf[RouteJsonFileVisitDataProvider])


      bind(classOf[UserDao])
        .to(classOf[PostgresBasedUserDao])
//      bind(classOf[UserDao])
//        .to(classOf[FileBasedUserDao])

    bind(classOf[Reader])
      .to(classOf[WebZipInputStream])
  }
}