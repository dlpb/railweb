package services.location

import com.typesafe.config.Config
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LocationServiceTest
  extends AnyFlatSpec with Matchers with MockitoSugar {

  val singleLocation = "/singleLocation.json"

  "Location Service" should "return a list of all locations in the json file" in {

    val service = getLocationServiceWith(singleLocation)

    val locations = service.locations

    locations.size should be(1)
  }

  private def getLocationServiceWith(routePath: String) = {
    val mockConfig = mock[Config]
    when(mockConfig.getString("data.routes.path")).thenReturn(routePath)
    val service = new LocationService(mockConfig)
    service
  }
}
