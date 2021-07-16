package services.location

import com.typesafe.config.Config
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LocationServiceTest
  extends AnyFlatSpec with Matchers with MockitoSugar {

  val singleLocation = "/singleLocation.json"
  val twoTiplocs = "/twoLocationsWithTheSameTiploc.json"

  "Location Service" should "return a list of all locations in the json file" in {

    val service = getLocationServiceWith(singleLocation)

    val locations = service.locations

    locations.size should be(1)
  }

  it should "find the first location with a given tiploc" in {
    val service = getLocationServiceWith(twoTiplocs)

    val location = service.findFirstLocationByTiploc("KGX")

    location should not be empty
    location.get.name should be("London Kings Cross original")
  }

  it should "find the first location with a given crs if its a station" in {
    val service = getLocationServiceWith("/twoCrsWithStation.json")

    val location = service.findFirstLocationByCrs("KGX")

    location should not be empty
    location.get.name should be("London Kings Cross original")
  }

  it should "find the second location with a given crs if  first is not a station" in {
    val service = getLocationServiceWith("/twoCrsOneNotStation.json")

    val location = service.findFirstLocationByCrs("KGX")

    location should not be empty
    location.get.name should be("London Kings Cross second")
  }

  it should "not find a location with a given crs if none a station" in {
    val service = getLocationServiceWith("/twoCrsNoStation.json")

    val location = service.findFirstLocationByCrs("KGX")

    location should be(empty)
  }

  it should "find a location by name" in {
    val service = getLocationServiceWith("/twoCrsNoStation.json")

    val location = service.findFirstLocationByNameTiplocCrsOrId("London Kings Cross original")

    location should not be empty
    location.get.name should be("London Kings Cross original")
  }

  it should "find a location by tiploc" in {
    val service = getLocationServiceWith("/twoCrsNoStation.json")

    val location = service.findFirstLocationByNameTiplocCrsOrId("LIVSTLL")

    location should not be empty
    location.get.name should be("London Liverpool Street")
  }

  it should "find a location by crs" in {
    val service = getLocationServiceWith("/twoCrsNoStation.json")

    val location = service.findFirstLocationByNameTiplocCrsOrId("LIX")

    location should not be empty
    location.get.name should be("London Liverpool Street")
  }

  it should "find a location by id" in {
    val service = getLocationServiceWith("/twoCrsNoStation.json")

    val location = service.findFirstLocationByNameTiplocCrsOrId("LST")

    location should not be empty
    location.get.name should be("London Liverpool Street")
  }

  it should "find matching locations prioritising ORR stations" in {
    val service = getLocationServiceWith("/twoCrsOneNotStation.json")

    val location = service.findLocationByNameTiplocCrsIdPrioritiseOrrStations("KGX")

    location should not be empty
    location.get.name should be("London Kings Cross second")
  }

  it should "find matching locations prioritising ORR stations if no ORR station is used" in {
    val service = getLocationServiceWith("/twoCrsNoStation.json")

    val location = service.findLocationByNameTiplocCrsIdPrioritiseOrrStations("KGX")

    location should not be empty
    location.get.name should be("London Kings Cross original")
  }

  it should "have a sorted list of locations by tiploc" in {
    val service = getLocationServiceWith("/twoLocationsWithTheSameTiploc.json")

    val locations = service.sortedLocationsGroupedByTiploc

    locations.size should be(3)
    locations.head.name should be("London Liverpool Street")

  }

//  it should "have a sorted list of locations by CRS" in {
//    val service = getLocationServiceWith("/twoCrsWithStation.json")
//
//    val locations = service.sortedListLocationsGroupedByCrs
//
//    locations.size should be(1)
//    locations.head.id should be("KGX")
//    locations.head.relatedLocations.size should be(2)
//  }

  private def getLocationServiceWith(routePath: String) = {
    val mockConfig = mock[Config]
    when(mockConfig.getString("data.locations.path")).thenReturn(routePath)
    val service = new LocationService(mockConfig)
    service
  }
}
