import java.time.LocalDate
import java.util.Date

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import data.{LocationMapBasedDataProvider, RouteMapBasedDataProvider}
import models.list.PathService
import models.location.LocationsService
import models.plan.TrainService
import models.route.RoutesService
import models.timetable._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.ws.{WSClient, WSRequest}

class DisplayTimetableTest extends FlatSpec with Matchers {

  val mockWsClient = new WSClient {
    override def underlying[T]: T = ???

    override def url(url: String): WSRequest = ???

    override def close(): Unit = ???
  }

  val locationService = new LocationsService(config, new LocationMapBasedDataProvider())
  val routeService = new RoutesService(config, new RouteMapBasedDataProvider())

  val pathService = new PathService(routeService, locationService)

  it should "map timetable to display timetable" in {

    val stt = createSimpleTimetableWithoutPass
    val dst = new DisplayTimetable(locationService, new TrainService(locationService, pathService, mockWsClient)).displaySimpleTimetable(stt, 2019, 1,1)

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

  it should "map timetable to display detailed timetable" in {

    val stt = createSimpleTimetableWithoutPass
    val dst = new DisplayTimetable(locationService, new TrainService(locationService, pathService, mockWsClient)).displayDetailedTimetable(stt, 2019, 1,1)

    dst.id should be("12345")
    dst.isPublic should be(true)
    dst.uid should be("12345")
    dst.isPass should be(false)
    dst.arrival should be("0921")
    dst.arrivalLabel should be("")
    dst.departure should be("0930")
    dst.departureLabel should be("")
    dst.platform should be("1")
    dst.platformLabel should be("")
    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.trainUrl should be("/plan/train/detailed/12345/2019/1/1")
  }

  it should "map timetable with pass to display detailed timetable" in {

    val stt = createSimpleTimetableWithPass
    val dst = new DisplayTimetable(locationService, new TrainService(locationService, pathService, mockWsClient)).displayDetailedTimetable(stt, 2019, 1,1)

    dst.id should be("12345")
    dst.isPublic should be(true)
    dst.uid should be("12345")
    dst.isPass should be(true)
    dst.arrival should be("pass")
    dst.arrivalLabel should be("")
    dst.departure should be("0931")
    dst.departureLabel should be("")
    dst.platform should be("")
    dst.platformLabel should be("")
    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.trainUrl should be("/plan/train/detailed/12345/2019/1/1")
  }

  it should "map timetable for non passenger to display detailed timetable" in {

    val stt = createNonPublicTrain()
    val dst = new DisplayTimetable(locationService, new TrainService(locationService, pathService, mockWsClient)).displayDetailedTimetable(stt, 2019, 1,1)

    dst.id should be("12345")
    dst.isPublic should be(false)
    dst.uid should be("12345")
    dst.isPass should be(true)
    dst.arrival should be("pass")
    dst.arrivalLabel should be("")
    dst.departure should be("0931")
    dst.departureLabel should be("")
    dst.platform should be("")
    dst.platformLabel should be("")
    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.trainUrl should be("/plan/train/detailed/12345/2019/1/1")
  }

  it should "map timetable to display timetable for train that starts at same location" in {

    val stt = createSimpleTimetableForStartingWithoutPass
    val dst = new DisplayTimetable(locationService, new TrainService(locationService, pathService, mockWsClient)).displaySimpleTimetable(stt, 2019, 1, 1)

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
    val dst = new DisplayTimetable(locationService, new TrainService(locationService, pathService, mockWsClient)).displaySimpleTimetable(stt, 2019, 1, 1)

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
    val date = LocalDate.now()
    val dst = new DisplayTimetable(locationService, new TrainService(locationService, pathService, mockWsClient))
      .displayIndividualTimetable(tt, date.getYear, date.getMonthValue, date.getDayOfMonth)

    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.operator should be("XR")
    dst.runningOn should be(date)
    dst.locations should be(List(
      DisplaySimpleTimetableLocation("London Liverpool Street", "", "", "1000", "Dep.", "1", "Platform", TrainService.createUrlForDisplayingLocationSimpleTimetables("LST", date.getYear, date.getMonthValue, date.getDayOfMonth, 945, 1045)),
      DisplaySimpleTimetableLocation("Cambridge", "1100", "Arr.", "1101", "Dep.", "1", "Platform", TrainService.createUrlForDisplayingLocationSimpleTimetables("CBG", date.getYear, date.getMonthValue, date.getDayOfMonth, 1045, 1145)),
      DisplaySimpleTimetableLocation("Kings Lynn", "1203", "Arr.", "", "", "1", "Platform", TrainService.createUrlForDisplayingLocationSimpleTimetables("KLN", date.getYear, date.getMonthValue, date.getDayOfMonth, 1148, 1248)),
    ))
  }

  it should "individual timetable to display detailed timetable" in {
    val tt = createIndividualTimetable()
    val date = LocalDate.now()
    val dst = new DisplayTimetable(locationService, new TrainService(locationService, pathService, mockWsClient))
      .displayDetailedIndividualTimetable(tt, date.getYear, date.getMonthValue, date.getDayOfMonth)

    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.operator should be("XR")
    dst.runningOn should be(date)
    dst.runningDays should be("MTWThFSSu")
    dst.bankHolidayRunning should be("RunsOnBankHolidays")
    dst.category should be("OrdinaryPassenger")
    dst.timing should be("DMUPowerCarAndTrailer")
    dst.status should be("PassengerAndParcelsPermanent")
    dst.powerType should be("Diesel")
    dst.speed should be("100")
    dst.operatingCharacteristics should be(List())
    dst.seating should be("FirstAndStandardSeating")
    dst.sleepers should be("NoSleeper")
    dst.reservations should be("ReservationsCompulsory")
    dst.catering should be(List())
    dst.branding should be("branding")
    dst.stpIndicator should be("New")
    dst.locations should be(List(
      DisplayDetailedIndividualTimetableLocation("LST","London Liverpool Street","1",false,"","1000","","","","","", "/plan/location/trains/detailed/LST?year=2019&month=08&day=10&from=0945&to=1045"),
      DisplayDetailedIndividualTimetableLocation("BIS","Bishops Stortford","",true,"pass","1030","","","","","", "/plan/location/trains/detailed/BIS?year=2019&month=08&day=10&from=1015&to=1115"),
      DisplayDetailedIndividualTimetableLocation("CBG","Cambridge","1",false,"1100","1101","","","","","", "/plan/location/trains/detailed/CBG?year=2019&month=08&day=10&from=1045&to=1145"),
      DisplayDetailedIndividualTimetableLocation("KLN","Kings Lynn","1",false,"1203","","","","","", "", "/plan/location/trains/detailed/KLN?year=2019&month=08&day=10&from=1148&to=1248"))
    )
  }

  it should "provide an arrival and departure time for intermediate locations" in {
    val tt = createIndividualTimetableWithMissingArrival()
    val date = LocalDate.now()
    val dst = new DisplayTimetable(locationService, new TrainService(locationService, pathService, mockWsClient))
      .displayIndividualTimetable(tt, date.getYear, date.getMonthValue, date.getDayOfMonth)

    dst.origin should be("London Liverpool Street")
    dst.destination should be("Kings Lynn")
    dst.operator should be("XR")
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
      )
    )
  }

}
