package models.timetable

import java.util.Date

case class Timetable(
                      tiplocs: List[Tiploc],
                      associations: List[Association],
                      timetables: List[IndividualTimetable]
                    )

case class Header(header: String)

case class IndividualTimetable(
                                basicSchedule: BasicSchedule,
                                basicScheduleExtraDetails: BasicScheduleExtraDetails,
                                locations: List[Location])

case class Association(transactionType: TransactionType,
                       mainTrainUid: String,
                       associatedTrainUid: String,
                       startDate: Date,
                       endDate: Date,
                       validMonday: Boolean,
                       validTuesday: Boolean,
                       validWednesday: Boolean,
                       validThursday: Boolean,
                       validFriday: Boolean,
                       validSaturday: Boolean,
                       validSunday: Boolean,
                       category: Category,
                       dateIndicator: DateIndicator,
                       location: String,
                       baseSuffixLocation: Char,
                       locationSuffix: Char,
                       associationType: AssociationType,
                       stpIndicator: StpIndicator
                      )

sealed trait TransactionType

case object NewTransaction extends TransactionType

case object DeleteTransaction extends TransactionType

case object ReviseTransaction extends TransactionType

sealed trait Category

case object Join extends Category

case object Divide extends Category

case object Next extends Category

case object NoCategory extends Category

sealed trait DateIndicator

case object StandardDaytimeOnly extends DateIndicator

case object OverNextMidnight extends DateIndicator

case object OverPreviousMidnight extends DateIndicator

case object NoDateIndicator extends DateIndicator

sealed trait AssociationType

case object Passenger extends AssociationType

case object Operating extends AssociationType

case object NoAssociationType extends AssociationType

sealed trait StpIndicator

case object Cancellation extends StpIndicator

case object New extends StpIndicator

case object Overlay extends StpIndicator

case object Permanent extends StpIndicator

case class Tiploc(tiploc: String, nlc: Int, nlcCheckChar: Char, tpsDesc: String, stanox: Int, crs: String, desc: String)

case class TiplocAmend(tiploc: String, nlc: Int, nlcCheckChar: Char, tpsDesc: String, stanox: Int, crs: String, desc: String, newTiploc: String)

sealed trait BankHolidayRunning

case object NotOnSpecifiedBankHolidayMondays extends BankHolidayRunning

case object NotOnGlasgowBankHolidays extends BankHolidayRunning

case object RunsOnBankHolidays extends BankHolidayRunning

sealed trait TrainStatus

case object BusPermanent extends TrainStatus

case object FreightPermanent extends TrainStatus

case object PassengerAndParcelsPermanent extends TrainStatus

case object ShipPermanent extends TrainStatus

case object TripPermanent extends TrainStatus

case object PassengerAndParcelsStp extends TrainStatus

case object FreightStp extends TrainStatus

case object TripStp extends TrainStatus

case object ShipStp extends TrainStatus

case object BusStp extends TrainStatus

sealed trait TrainCategory

case object OrdinaryLondonUndergroundMetroService extends TrainCategory

case object UnadvertisedOrdinaryPassenger extends TrainCategory

case object OrdinaryPassenger extends TrainCategory

case object OrdinaryStaffTrain extends TrainCategory

case object OrdinaryMixed extends TrainCategory

case object ExpressChannelTunnel extends TrainCategory

case object ExpressSleeperEuropeNightServices extends TrainCategory

case object ExpressInternational extends TrainCategory

case object ExpressMotorail extends TrainCategory

case object UnadvertisedExpress extends TrainCategory

case object ExpressPassenger extends TrainCategory

case object ExpressSleeperDomestic extends TrainCategory

case object BusReplacement extends TrainCategory

case object BusWtt extends TrainCategory

case object Ship extends TrainCategory

case object EmptyCoachingStock extends TrainCategory

case object EmptyCoachingStockLondonUndergroundMetroService extends TrainCategory

case object EmptyCoachingStockStaff extends TrainCategory

case object Postal extends TrainCategory

case object PostOfficeControlledParcels extends TrainCategory

case object Parcels extends TrainCategory

case object EmptyParcels extends TrainCategory

case object Departmental extends TrainCategory

case object CivilEngineer extends TrainCategory

case object MechanicalAndElectricalEngineer extends TrainCategory

case object Stores extends TrainCategory

case object Test extends TrainCategory

case object SignalAndTelecommunicationsEngineer extends TrainCategory

case object LocomotiveAndBrakeVan extends TrainCategory

case object Locomotive extends TrainCategory

case object RFDAutomotiveComponents extends TrainCategory

case object RFDAutomotiveVehicles extends TrainCategory

case object RFDEdibleProductsUKContracts extends TrainCategory

case object RFDIndustrialMineralsUKContracts extends TrainCategory

case object RFDChemicalsUkContracts extends TrainCategory

case object RFDBuildingMaterialsUKContracts extends TrainCategory

case object RFDGeneralMerchandiseUKContracts extends TrainCategory

case object RFDEuropean extends TrainCategory

case object RFDFreightlinerContracts extends TrainCategory

case object RFDFreightlinerOther extends TrainCategory

case object CoalDistributive extends TrainCategory

case object CoalElectricity extends TrainCategory

case object CoalOtherAndNuclear extends TrainCategory

case object Metals extends TrainCategory

case object Aggregates extends TrainCategory

case object DomesticAndIndustrialWaste extends TrainCategory

case object BuildingMaterials extends TrainCategory

case object PetroleumProducts extends TrainCategory

case object RFDEuropeanChannelTunnelMixedBusiness extends TrainCategory

case object RFDEuropeanChannelTunnelIntermodal extends TrainCategory

case object RFDEuropeanChannelTunnelAutomotive extends TrainCategory

case object RFDEuropeanChannelTunnelContractServices extends TrainCategory

case object RFDEuropeanChannelTunnelHaulmark extends TrainCategory

case object RFDEuropeanChannelTunnelJointVenture extends TrainCategory

case class OtherTrainCategory(category: String) extends TrainCategory

sealed trait PowerType

case object Diesel extends PowerType

case object DieselElectricMultipleUnit extends PowerType

case object DieselMechanicalMultipleUnit extends PowerType

case object Electric extends PowerType

case object ElectroDiesel extends PowerType

case object ElectricMultipleUnitPlusLocomotive extends PowerType

case object ElectricMultipleUnit extends PowerType

case object HighSpeedTrain extends PowerType

case object NoTrainPowerType extends PowerType

sealed trait Timing

case object DMUPowerCarAndTrailer extends Timing

case object DMUTwoPowerCarsAndTrailer extends Timing

case object DMUPowerTwin extends Timing

case object EmuAcceleratedTimings extends Timing

case object ElectricParcelsUnit extends Timing

case class Hauled(load: Int) extends Timing

case class Class(unitClass: String*) extends Timing

case object NoTiming extends Timing

sealed trait OperatingCharacteristics

case object VacuumBraked extends OperatingCharacteristics

case object TimedAt100Mph extends OperatingCharacteristics

case object DriverOnlyOperatedCoachingStock extends OperatingCharacteristics

case object ConveysMark4Coaches extends OperatingCharacteristics

case object TrainmanGuardRequired extends OperatingCharacteristics

case object TimedAt110Mph extends OperatingCharacteristics

case object PushPullTrain extends OperatingCharacteristics

case object RunsAsRequired extends OperatingCharacteristics

case object AirConditionedWithPaSystem extends OperatingCharacteristics

case object SteamHauled extends OperatingCharacteristics

case object RunsToTerminalsYardsAsRequired extends OperatingCharacteristics

case object SB1CGaugeNoDiversionWithoutAuthority extends OperatingCharacteristics

case object NoOperatingCharacteristics extends OperatingCharacteristics

sealed trait Seating

case object FirstAndStandardSeating extends Seating

case object StandardOnlySeating extends Seating

sealed trait Sleepers

case object FirstAndStandardClassSleeper extends Sleepers

case object FirstClassOnlySleeper extends Sleepers

case object StandardClassOnlySleeper extends Sleepers

case object NoSleeper extends Sleepers

sealed trait Reservations

case object ReservationsCompulsory extends Reservations

case object ReservationsForBicycles extends Reservations

case object ReservationsRecommended extends Reservations

case object ReservationsPossibleFromAnyStation extends Reservations

case object NoReservations extends Reservations

sealed trait Catering

case object BuffetServiceCatering extends Catering

case object RestaurantCarFirstClassCatering extends Catering

case object HotFoodCatering extends Catering

case object MealIncludedFirstClassCatering extends Catering

case object WheelchairOnlyReservationsCatering extends Catering

case object RestaurantCatering extends Catering

case object TrolleyServiceCatering extends Catering

case object NoCatering extends Catering

case class BasicSchedule(
                          transactionType: TransactionType,
                          trainUid: String,
                          runsFrom: Date,
                          runsTo: Date,
                          validMonday: Boolean,
                          validTuesday: Boolean,
                          validWednesday: Boolean,
                          validThursday: Boolean,
                          validFriday: Boolean,
                          validSaturday: Boolean,
                          validSunday: Boolean,
                          bankHolidayRunning: BankHolidayRunning,
                          trainStatus: TrainStatus,
                          trainCategory: TrainCategory,
                          trainIdentity: String,
                          headcode: String,
                          trainServiceCode: String,
                          powerType: PowerType,
                          timing: Timing,
                          speed: Int,
                          operatingCharacteristics: List[OperatingCharacteristics],
                          seating: Seating,
                          sleepers: Sleepers,
                          reservations: Reservations,
                          catering: List[Catering],
                          branding: String,
                          stpIndicator: StpIndicator

                        )

sealed trait TimetableCode

case object SubjectToMonitoring extends TimetableCode

case object NotSubjectToMonitoring extends TimetableCode

case class BasicScheduleExtraDetails(
                                      atocCode: String
                                    )


trait Location {
  def tiploc: String

  def platform: String
  def line: String

  def engineeringAllowance: Int
  def engineeringAllowanceHalfMinute: Boolean

  def pathingAllowance: Int
  def pathingAllowanceHalfMinute: Boolean

  def performanceAllowance: Int
  def performanceAllowanceHalfMinute: Boolean

  def departure: Option[Int]
  def departureHalfMinute: Option[Boolean]

  def arrival: Option[Int]
  def arrivalHalfMinute: Option[Boolean]

  def pass: Option[Int]
  def passHalfMinute: Option[Boolean]

  def path: Option[String]

  def publicArrival: Option[Int]
  def publicDeparture: Option[Int]


}

case class LocationOrigin(override val tiploc: String,
                          override val platform: String,
                          override val line: String,
                          override val engineeringAllowance: Int,
                          override val engineeringAllowanceHalfMinute: Boolean,
                          override val pathingAllowance: Int,
                          override val pathingAllowanceHalfMinute: Boolean,
                          override val performanceAllowance: Int,
                          override val performanceAllowanceHalfMinute: Boolean,
                          override val departure: Option[Int],
                          override val departureHalfMinute: Option[Boolean],
                          override val publicDeparture: Option[Int])
  extends Location {
  override def arrival: Option[Int] = None
  override def arrivalHalfMinute: Option[Boolean] = None
  override def pass: Option[Int] = None
  override def passHalfMinute: Option[Boolean] = None
  override def path: Option[String] = None
  override def publicArrival: Option[Int] = None
}

case class LocationIntermediate(override val tiploc: String,
                                override val platform: String,
                                override val line: String,
                                override val engineeringAllowance: Int,
                                override val engineeringAllowanceHalfMinute: Boolean,
                                override val pathingAllowance: Int,
                                override val pathingAllowanceHalfMinute: Boolean,
                                override val performanceAllowance: Int,
                                override val performanceAllowanceHalfMinute: Boolean,
                                override val arrival: Option[Int],
                                override val arrivalHalfMinute: Option[Boolean],
                                override val departure: Option[Int],
                                override val departureHalfMinute: Option[Boolean],
                                override val pass: Option[Int],
                                override val passHalfMinute: Option[Boolean],
                                override val path:  Option[String],
                                override val publicArrival: Option[Int],
                                override val publicDeparture: Option[Int])
  extends Location

case class LocationTerminal(override val tiploc: String,
                            override val platform: String,
                            override val line: String,
                            override val engineeringAllowance: Int,
                            override val engineeringAllowanceHalfMinute: Boolean,
                            override val pathingAllowance: Int,
                            override val pathingAllowanceHalfMinute: Boolean,
                            override val performanceAllowance: Int,
                            override val performanceAllowanceHalfMinute: Boolean,
                            override val arrival: Option[Int],
                            override val arrivalHalfMinute: Option[Boolean],
                            override val path: Option[String],
                            override val publicArrival: Option[Int])
  extends Location {
  override def departure: Option[Int] = None
  override def departureHalfMinute: Option[Boolean] = None
  override def pass: Option[Int] = None
  override def passHalfMinute: Option[Boolean] = None
  override def publicDeparture: Option[Int] = None
}
