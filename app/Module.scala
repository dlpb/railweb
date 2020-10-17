import com.google.inject.AbstractModule
import com.typesafe.config.Config
import models.auth.{FileBasedUserDao, PostgresBasedUserDao, UserDao}
import models.data._
import models.data.postgres.{LocationJsonPostgresDataProvider, RouteJsonPostgresDataProvider}
import models.plan.timetable.reader.{Reader, WebZipInputStream}

class Module extends AbstractModule {
  override def configure() = {

//    bind(classOf[LocationDataProvider])
//      .to(classOf[LocationJsonPostgresDataProvider])
//
//    bind(classOf[RouteDataProvider])
//      .to(classOf[RouteJsonPostgresDataProvider])
//
    bind(classOf[LocationDataProvider])
      .to(classOf[LocationJsonFileDataProvider])

    bind(classOf[RouteDataProvider])
      .to(classOf[RouteJsonFileDataProvider])


//      bind(classOf[UserDao])
//        .to(classOf[PostgresBasedUserDao])
      bind(classOf[UserDao])
        .to(classOf[FileBasedUserDao])

    bind(classOf[Reader])
      .to(classOf[WebZipInputStream])
  }
}