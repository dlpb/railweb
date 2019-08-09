import java.io.{ByteArrayInputStream, InputStream}
import java.time.LocalDate
import java.util.Date

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import data.{LocationMapBasedDataProvider, RouteMapBasedDataProvider}
import models.list.PathService
import models.location.{LocationsService, MapLocation, SpatialLocation}
import models.plan.{PlanService, Reader}
import models.route.RoutesService
import models.timetable._
import org.scalatest.{FlatSpec, Matchers}

class PlanTest extends FlatSpec with Matchers {

  val locationService = new LocationsService(config, new LocationMapBasedDataProvider())
  val routeService = new RoutesService(config, new RouteMapBasedDataProvider())

  val pathService = new PathService(routeService, locationService)

  "Plan Service" should "create a padded url for reading location timetables" in {
    val url = PlanService.createUrlForReadingLocationTimetables("CTH", 2019, 1, 1, 30, 900)
    url should be("http://railweb-timetables.herokuapp.com/timetables/location/CTH?year=2019&month=01&day=01&from=0030&to=0900")
  }

  it should "create an unpadded url for reading location timetables" in {
    val url = PlanService.createUrlForReadingLocationTimetables("CTH", 2019, 10, 11, 2030, 2200)
    url should be("http://railweb-timetables.herokuapp.com/timetables/location/CTH?year=2019&month=10&day=11&from=2030&to=2200")
  }

  it should "get timetable for around now" in {
    val from = PlanService.from
    val to = PlanService.to

    val fromTime = from.getHour * 100 + from.getMinute
    val toTime = to.getHour * 100 + to.getMinute

    val expectedUrl = PlanService.createUrlForReadingLocationTimetables("CTH", from.getYear, from.getMonthValue, from.getDayOfMonth, fromTime, toTime)

    val service = new PlanService(locationService, pathService, new Reader {
      override def getInputStream(url: String): InputStream = {
        url should be(expectedUrl)
        new ByteArrayInputStream("".getBytes)
      }
    })

    service.getTrainsForLocationAroundNow("CTH")
  }

  it should "extract hour minute fro int" in {
    PlanService.hourMinute(0) should be((0, 0))
    PlanService.hourMinute(30) should be((0, 30))
    PlanService.hourMinute(103) should be((1, 3))
    PlanService.hourMinute(2013) should be((20, 13))
  }

  it should "create url for reading specific train timetable" in {
    PlanService.createUrlForReadingTrainTimetable("train") should be("http://railweb-timetables.herokuapp.com/timetables/train/train")
  }

  it should "get timetable for specific train" in {
    val expectedUrl = PlanService.createUrlForReadingTrainTimetable("train")

    val service = new PlanService(locationService, pathService, new Reader {
      override def getInputStream(url: String): InputStream = {
        url should be(expectedUrl)
        new ByteArrayInputStream("".getBytes)
      }
    })

    service.getTrain("CTH")
  }

  it should "map timetable to display timetable" in {

    val stt = createSimpleTimetableWithoutPass
    val dst = new DisplayTimetable(locationService, new PlanService(locationService, pathService)).displaySimpleTimetable(stt, 2019, 1,1)

    dst.arrival should be("0921")
    dst.arrivalLabel should be("Arr.")
    dst.departure should be("0930")
    dst.departureLabel should be("Dep.")
    dst.platform should be("1")
    dst.platformLabel should be("Platform")
    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.trainUrl should be("/plan/train/simple/12345/2019/1/1")
  }

  it should "map timetable to display timetable for train that starts at same location" in {

    val stt = createSimpleTimetableForStartingWithoutPass
    val dst = new DisplayTimetable(locationService, new PlanService(locationService, pathService)).displaySimpleTimetable(stt, 2019, 1, 1)

    dst.arrival should be("")
    dst.arrivalLabel should be("")
    dst.departure should be("1000")
    dst.departureLabel should be("Dep.")
    dst.platform should be("1")
    dst.platformLabel should be("Platform")
    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.trainUrl should be("/plan/train/simple/12345/2019/1/1")
  }

  it should "map timetable to display timetable for train that ends at same location" in {

    val stt = createSimpleTimetableForEndingWithoutPass
    val dst = new DisplayTimetable(locationService, new PlanService(locationService, pathService)).displaySimpleTimetable(stt, 2019, 1, 1)

    dst.arrival should be("1000")
    dst.arrivalLabel should be("Arr.")
    dst.departure should be("")
    dst.departureLabel should be("")
    dst.platform should be("1")
    dst.platformLabel should be("Platform")
    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.trainUrl should be("/plan/train/simple/12345/2019/1/1")
  }

  it should "individual timetable to display timetable" in {
    val tt = createIndividualTimetable()
    val dst = new DisplayTimetable(locationService, new PlanService(locationService, pathService)).displayIndividualTimetable(tt)

    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.operator should be("XR")
    val date = LocalDate.now()
    dst.runningOn should be(date)
    dst.locations should be(List(
      DisplaySimpleTimetableLocation("London Liverpool Street", "", "", "1000", "Dep.", "1", "Platform", PlanService.createUrlForDisplayingLocationSimpleTimetables("LST", date.getYear, date.getMonthValue, date.getDayOfMonth, 945, 1045)),
      DisplaySimpleTimetableLocation("Cambridge", "1100", "Arr.", "1101", "Dep.", "1", "Platform", PlanService.createUrlForDisplayingLocationSimpleTimetables("CBG", date.getYear, date.getMonthValue, date.getDayOfMonth, 1045, 1145)),
      DisplaySimpleTimetableLocation("Kings Lynn", "1203", "Arr.", "", "", "1", "Platform", PlanService.createUrlForDisplayingLocationSimpleTimetables("KLN", date.getYear, date.getMonthValue, date.getDayOfMonth, 1148, 1248)),
    ))
  }

  it should "get the route and station points for a simple timetable" in {
    val tt = createIndividualTimetable()

    val service = new PlanService(locationService, pathService)


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
    timetables.filter(t => PlanService.filterPasses(t)) should have length 0
  }

  it should "filter out non public trains" in {
    val timetables = List(createNonPublicTrain)
    timetables.filter(t => PlanService.filterNonPassengerTrains(t)) should have length 0
  }

  it should "provide an arrival and departure time for intermediate locations" in {
    val tt = createIndividualTimetableWithMissingArrival()
    val dst = new DisplayTimetable(locationService, new PlanService(locationService, pathService)).displayIndividualTimetable(tt)

    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.operator should be("XR")
    val date = LocalDate.now()
    dst.runningOn should be(date)
    dst.locations.map(l => (l.arrival, l.departure)) should be(List(
      ("","1000"),
      ("0933","0933"),
      ("1100","1101"),
      ("1203","")
   ))
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

  private def createIndividualTimetableWithMissingArrival() = {
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
          Some(930),
          None,
          Some(932),
          None,
          None,
          None,
          None,
          None,
          Some(933)
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

  private def createSimpleTimetableWithoutPass
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
        Some(1028),
        None,
        Some(1031),
        None,
        None,
        None,
        None,
        Some(921),
        Some(930)

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

  private def createSimpleTimetableForEndingWithoutPass
  = {
    new SimpleTimetable(
      basicSchedule = new BasicSchedule(
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
      origin = new LocationOrigin(
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
      location = new LocationTerminal(
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
      ),
      destination = new LocationTerminal(
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

  private def createSimpleTimetableForStartingWithoutPass
  = {
    new SimpleTimetable(
      basicSchedule = new BasicSchedule(
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
      origin = new LocationOrigin(
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
      location = new LocationOrigin(
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
      destination = new LocationTerminal(
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
