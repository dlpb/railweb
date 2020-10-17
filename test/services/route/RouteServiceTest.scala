package services.route

import com.typesafe.config.Config
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RouteServiceTest
  extends AnyFlatSpec with Matchers with MockitoSugar {

  "Route Service" should "return a list of all routes in the json file" in {

    val mockConfig = mock[Config]
    when(mockConfig.getString("data.static.root")).thenReturn("test/resources/singleRoute.json")

    val service = new RouteService(mockConfig)

    val routes = service.getAllRoutes()

    routes.size should be(1)
  }

}
