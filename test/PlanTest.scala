import java.io.{ByteArrayInputStream, InputStream}
import java.util.Date

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import data.{LocationMapBasedDataProvider, RouteMapBasedDataProvider}
import models.location.{LocationsService, MapLocation}
import models.plan.route.pointtopoint.PathService
import models.plan.{Reader, TrainService}
import models.route.RoutesService
import models.timetable._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.ws.{WSClient, WSRequest}

class PlanTest extends FlatSpec with Matchers {

  val mockWsClient = new WSClient {
    override def underlying[T]: T = ???

    override def url(url: String): WSRequest = ???

    override def close(): Unit = ???
  }

  val locationService = new LocationsService(config, new LocationMapBasedDataProvider())
  val routeService = new RoutesService(config, new RouteMapBasedDataProvider())

  val pathService = new PathService(routeService, locationService)

  "Plan Service" should "create a padded url for reading location timetables" in {
    val url = TrainService.createUrlForReadingLocationTimetables("CTH", 2019, 1, 1, 30, 900)
    url should be("http://railweb-timetables.herokuapp.com/timetables/location/CTH?year=2019&month=01&day=01&from=0030&to=0900")
  }

  it should "create an unpadded url for reading location timetables" in {
    val url = TrainService.createUrlForReadingLocationTimetables("CTH", 2019, 10, 11, 2030, 2200)
    url should be("http://railweb-timetables.herokuapp.com/timetables/location/CTH?year=2019&month=10&day=11&from=2030&to=2200")
  }

  it should "get timetable for around now" in {
    val from = TrainService.from
    val to = TrainService.to

    val fromTime = from.getHour * 100 + from.getMinute
    val toTime = to.getHour * 100 + to.getMinute

    val expectedUrl = TrainService.createUrlForReadingLocationTimetables("CTH", from.getYear, from.getMonthValue, from.getDayOfMonth, fromTime, toTime)

    val service = new TrainService(locationService, pathService, mockWsClient, new Reader {
      override def getInputStream(url: String): InputStream = {
        url should be(expectedUrl)
        new ByteArrayInputStream("".getBytes)
      }
    })

    service.getTrainsForLocationAroundNow("CTH")
  }

  it should "extract hour minute fro int" in {
    TrainService.hourMinute(0) should be((0, 0))
    TrainService.hourMinute(30) should be((0, 30))
    TrainService.hourMinute(103) should be((1, 3))
    TrainService.hourMinute(2013) should be((20, 13))
  }

  it should "create url for reading specific train timetable" in {
    TrainService.createUrlForReadingTrainTimetable("train") should be("http://railweb-timetables.herokuapp.com/timetables/train/train")
  }

  it should "get timetable for specific train" in {
    val expectedUrl = TrainService.createUrlForReadingTrainTimetable("train")

    val service = new TrainService(locationService, pathService, mockWsClient, new Reader {
      override def getInputStream(url: String): InputStream = {
        url should be(expectedUrl)
        new ByteArrayInputStream("".getBytes)
      }
    })

    service.getTrain("CTH")
  }

  it should "get the route and station points for a simple timetable" in {
    val tt = createIndividualTimetable()

    val service = new TrainService(locationService, pathService, mockWsClient)


    val mapLocations = service.createSimpleMapLocations(tt)
    val mapRoutes = service.createSimpleMapRoutes(tt)

    mapLocations should be(List(
      MapLocation(locationService.getLocation("LST").get),
      MapLocation(locationService.getLocation("CBG").get),
      MapLocation(locationService.getLocation("KLN").get)
    ))

    mapRoutes should have length 56
  }

  it should "filter out trains that are passing" in {
    val timetables = List(createSimpleTimetableWithPass)
    timetables.filter(t => TrainService.isNotPass(t)) should have length 0
  }

  it should "filter out non public trains" in {
    val timetables = List(createNonPublicTrain)
    timetables.filter(t => TrainService.isPassengerTrain(t)) should have length 0
  }


  private def config = {
    val config = ConfigFactory
      .empty()
      .withValue("data.static.root", ConfigValueFactory.fromAnyRef(getClass().getResource("locations.json").getPath))
    config
  }

  private def createNonPublicTrain() = {
    SimpleTimetable(
      basicSchedule = BasicSchedule(
        NewTransaction,
        "12345",
        new Date(System.currentTimeMillis()),
        new Date(System.currentTimeMillis()),
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        RunsOnBankHolidays,
        PassengerAndParcelsPermanent,
        EmptyCoachingStock,
        "12345",
        "12345",
        "12345",
        Diesel,
        DMUPowerCarAndTrailer,
        100,
        List.empty,
        FirstAndStandardSeating,
        NoSleeper,
        ReservationsCompulsory,
        List.empty,
        "branding",
        New
      ),
      null, null, null
    )
  }

  private def createIndividualTimetable() = {
    IndividualTimetable(
      basicSchedule = BasicSchedule(
        NewTransaction,
        "12345",
        new Date(System.currentTimeMillis()),
        new Date(System.currentTimeMillis()),
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        RunsOnBankHolidays,
        PassengerAndParcelsPermanent,
        OrdinaryPassenger,
        "12345",
        "12345",
        "12345",
        Diesel,
        DMUPowerCarAndTrailer,
        100,
        List.empty,
        FirstAndStandardSeating,
        NoSleeper,
        ReservationsCompulsory,
        List.empty,
        "branding",
        New
      ),
      basicScheduleExtraDetails = BasicScheduleExtraDetails("XR"),
      locations = List(
        LocationOrigin(
          "LIVST",
          "1",
          "",
          0,
          false,
          0,
          false,
          0,
          false,
          Some(1000),
          None,
          Some(1000)
        ),
        LocationIntermediate(
          "BIS",
          "1",
          "",
          0,
          false,
          0,
          false,
          0,
          false,
          None,
          None,
          None,
          None,
          Some(1030),
          None,
          None,
          None,
          None
        ),
        LocationIntermediate(
          "CBG",
          "1",
          "",
          0,
          false,
          0,
          false,
          0,
          false,
          Some(1050),
          None,
          Some(1101),
          None,
          None,
          None,
          None,
          Some(1100),
          Some(1101)
        ),
        LocationTerminal(
          "KLN",
          "1",
          "",
          0,
          false,
          0,
          false,
          0,
          false,
          Some(1200),
          None,
          None,
          Some(1203)
        )
      )
    )
  }


  private def createSimpleTimetableWithPass
  = {
    SimpleTimetable(
      basicSchedule = BasicSchedule(
        NewTransaction,
        "12345",
        new Date(System.currentTimeMillis()),
        new Date(System.currentTimeMillis()),
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        RunsOnBankHolidays,
        PassengerAndParcelsPermanent,
        OrdinaryPassenger,
        "12345",
        "12345",
        "12345",
        Diesel,
        DMUPowerCarAndTrailer,
        100,
        List.empty,
        FirstAndStandardSeating,
        NoSleeper,
        ReservationsCompulsory,
        List.empty,
        "branding",
        New

      ),
      origin = LocationOrigin(
        "LIVST",
        "1",
        "",
        0,
        false,
        0,
        false,
        0,
        false,
        Some(1000),
        None,
        Some(1000)
      ),
      location = LocationIntermediate(
        "CBG",
        "1",
        "",
        0,
        false,
        0,
        false,
        0,
        false,
        None,
        None,
        None,
        None,
        Some(931),
        None,
        None,
        None,
        None

      ),
      destination = LocationTerminal(
        "KLN",
        "1",
        "",
        0,
        false,
        0,
        false,
        0,
        false,
        Some(1000),
        None,
        None,
        Some(1000)
      ))
  }



}
