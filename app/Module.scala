import com.google.inject.AbstractModule
import models.auth.{PostgresBasedUserDao, UserDao}
import models.data._
import models.data.postgres.{LocationJsonPostgresDataProvider, RouteJsonPostgresDataProvider}

class Module extends AbstractModule {
  override def configure() = {

    bind(classOf[LocationDataProvider])
      .to(classOf[LocationJsonPostgresDataProvider])

    bind(classOf[RouteDataProvider])
      .to(classOf[RouteJsonPostgresDataProvider])

    bind(classOf[UserDao])
      .to(classOf[PostgresBasedUserDao])
  }
}