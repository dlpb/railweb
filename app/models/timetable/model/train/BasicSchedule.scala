package models.timetable.model.train

import java.util.Date


sealed trait BankHolidayRunning extends FormattedToString

case object NotOnSpecifiedBankHolidayMondays extends BankHolidayRunning

case object NotOnGlasgowBankHolidays extends BankHolidayRunning

case object RunsOnBankHolidays extends BankHolidayRunning



sealed trait TrainStatus extends FormattedToString

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

case object NoTrainStatus extends TrainStatus



sealed trait TrainCategory extends FormattedToString

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



sealed trait PowerType extends FormattedToString

case object Diesel extends PowerType

case object DieselElectricMultipleUnit extends PowerType

case object DieselMechanicalMultipleUnit extends PowerType

case object Electric extends PowerType

case object ElectroDiesel extends PowerType

case object ElectricMultipleUnitPlusLocomotive extends PowerType

case object ElectricMultipleUnit extends PowerType

case object HighSpeedTrain extends PowerType

case object NoTrainPowerType extends PowerType



sealed trait Timing extends FormattedToString

case object DMUPowerCarAndTrailer extends Timing

case object DMUTwoPowerCarsAndTrailer extends Timing

case object DMUPowerTwin extends Timing

case object EmuAcceleratedTimings extends Timing

case object ElectricParcelsUnit extends Timing

case class Hauled(load: Int) extends Timing {
  override def toString: String = s"Hauled, trailing load ${load} tons"
}

case class Class(unitClass: String*) extends Timing {
  override def toString: String = unitClass.map(classs => s"Class ${classs}").mkString(",")
}

case object NoTiming extends Timing



sealed trait OperatingCharacteristics extends FormattedToString

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



sealed trait Seating extends FormattedToString

case object FirstAndStandardSeating extends Seating

case object StandardOnlySeating extends Seating


sealed trait Sleepers extends FormattedToString

case object FirstAndStandardClassSleeper extends Sleepers

case object FirstClassOnlySleeper extends Sleepers

case object StandardClassOnlySleeper extends Sleepers

case object NoSleeper extends Sleepers


sealed trait Reservations extends FormattedToString

case object ReservationsCompulsory extends Reservations

case object ReservationsForBicycles extends Reservations

case object ReservationsRecommended extends Reservations

case object ReservationsPossibleFromAnyStation extends Reservations

case object NoReservations extends Reservations

sealed trait Catering extends FormattedToString

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
                          portionId: String,
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

