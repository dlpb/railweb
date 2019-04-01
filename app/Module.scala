import com.google.inject.AbstractModule
import models.data._

class Module extends AbstractModule {
  override def configure() = {

    bind(classOf[LocationDataProvider])
      .to(classOf[LocationJsonFileDataProvider])

    bind(classOf[RouteDataProvider])
      .to(classOf[RouteJsonFileDataProvider])
  }
}