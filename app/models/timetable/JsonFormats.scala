package models.timetable

import org.json4s.CustomSerializer
import org.json4s.JsonAST._

object JsonFormats {
  val locationFormats = Seq(LocationSerializer)
  val formats = Seq(
    TransactionTypeSerializer,
    CategorySerializer,
    DateIndicatorSerializer,
    AssociationTypeSerializer,
    StpIndicatorSerializer,
    BankHolidayRunningSerializer,
    TrainStatusSerializer,
    TrainCategorySerializer,
    PowerTypeSerializer,
    TimingSerializer,
    OperatingCharacteristicsSerializer,
    SeatingSerializer,
    SleepersSerializer,
    ReservationsSerializer,
    CateringSerializer)

  val allFormats = formats ++ locationFormats

}

case object LocationSerializer extends CustomSerializer[Location](format => ({
  //origin
  case JObject(
    JField("tiploc", JString(t)) ::
      JField("platform", JString(p)) ::
      JField("line", JString(l)) ::
      JField("engineeringAllowance", JInt(ea)) ::
      JField("engineeringAllowanceHalfMinute", JBool(eah)) ::
      JField("pathingAllowance", JInt(pa)) ::
      JField("pathingAllowanceHalfMinute", JBool(pah)) ::
      JField("performanceAllowance", JInt(pea)) ::
      JField("performanceAllowanceHalfMinute", JBool(peh)) ::
      JField("departure", JInt(d)) ::
      JField("departureHalfMinute", JBool(dh)) ::
      JField("publicDeparture", JInt(pd)) :: Nil
    ) => LocationOrigin(t, p, l, ea.intValue(), eah, pa.intValue(), pah, pea.intValue(), peh, Some(d.intValue()), Some(dh), Some(pd.intValue()))

  //intermediate pass
  case JObject(
    JField("tiploc", JString(t)) ::
      JField("platform", JString(p)) ::
      JField("line", JString(l)) ::
      JField("engineeringAllowance", JInt(ea)) ::
      JField("engineeringAllowanceHalfMinute", JBool(eah)) ::
      JField("pathingAllowance", JInt(pa)) ::
      JField("pathingAllowanceHalfMinute", JBool(pah)) ::
      JField("performanceAllowance", JInt(pea)) ::
      JField("performanceAllowanceHalfMinute", JBool(peh)) ::
      JField("pass", JInt(pass)) ::
      JField("passHalfMinute", JBool(passh)) ::
      JField("path", JString(pth)) :: Nil
    ) => LocationIntermediate(t, p, l, ea.intValue(), eah, pa.intValue(), pah, pea.intValue(), peh, None, None, None, None, Some(pass.intValue()), Some(passh), Some(pth), None, None)

  //intermediate stop - public
  case JObject(
    JField("tiploc", JString(t)) ::
      JField("platform", JString(p)) ::
      JField("line", JString(l)) ::
      JField("engineeringAllowance", JInt(ea)) ::
      JField("engineeringAllowanceHalfMinute", JBool(eah)) ::
      JField("pathingAllowance", JInt(pa)) ::
      JField("pathingAllowanceHalfMinute", JBool(pah)) ::
      JField("performanceAllowance", JInt(pe)) ::
      JField("performanceAllowanceHalfMinute", JBool(peh)) ::
      JField("arrival", JInt(a)) ::
      JField("arrivalHalfMinute", JBool(ah)) ::
      JField("departure", JInt(d)) ::
      JField("departureHalfMinute", JBool(dh)) ::
      JField("path", JString(pth)) ::
      JField("publicArrival", JInt(par)) ::
      JField("publicDeparture", JInt(pd)) :: Nil
    ) => LocationIntermediate(t, p, l, ea.intValue(), eah, pa.intValue(), pah, pe.intValue(), peh, Some(a.intValue()), Some(ah), Some(d.intValue()), Some(dh), None, None, Some(pth), Some(par.intValue()), Some(pd.intValue()))

  //intermediate stop - public dep only
  case JObject(
    JField("tiploc", JString(t)) ::
      JField("platform", JString(p)) ::
      JField("line", JString(l)) ::
      JField("engineeringAllowance", JInt(ea)) ::
      JField("engineeringAllowanceHalfMinute", JBool(eah)) ::
      JField("pathingAllowance", JInt(pa)) ::
      JField("pathingAllowanceHalfMinute", JBool(pah)) ::
      JField("performanceAllowance", JInt(pe)) ::
      JField("performanceAllowanceHalfMinute", JBool(peh)) ::
      JField("arrival", JInt(a)) ::
      JField("arrivalHalfMinute", JBool(ah)) ::
      JField("departure", JInt(d)) ::
      JField("departureHalfMinute", JBool(dh)) ::
      JField("path", JString(pth)) ::
      JField("publicDeparture", JInt(pd)) :: Nil
    ) => LocationIntermediate(t, p, l, ea.intValue(), eah, pa.intValue(), pah, pe.intValue(), peh, Some(a.intValue()), Some(ah), Some(d.intValue()), Some(dh), None, None, Some(pth), None, Some(pd.intValue()))

  //intermediate stop - public dep only
  case JObject(
    JField("tiploc", JString(t)) ::
      JField("platform", JString(p)) ::
      JField("line", JString(l)) ::
      JField("engineeringAllowance", JInt(ea)) ::
      JField("engineeringAllowanceHalfMinute", JBool(eah)) ::
      JField("pathingAllowance", JInt(pa)) ::
      JField("pathingAllowanceHalfMinute", JBool(pah)) ::
      JField("performanceAllowance", JInt(pe)) ::
      JField("performanceAllowanceHalfMinute", JBool(peh)) ::
      JField("arrival", JInt(a)) ::
      JField("arrivalHalfMinute", JBool(ah)) ::
      JField("departure", JInt(d)) ::
      JField("departureHalfMinute", JBool(dh)) ::
      JField("path", JString(pth)) ::
      JField("publicArrival", JInt(par)) :: Nil
    ) => LocationIntermediate(t, p, l, ea.intValue(), eah, pa.intValue(), pah, pe.intValue(), peh, Some(a.intValue()), Some(ah), Some(d.intValue()), Some(dh), None, None, Some(pth), Some(par.intValue()), None)

  //intermediate stop - timing
  case JObject(
    JField("tiploc", JString(t)) ::
      JField("platform", JString(p)) ::
      JField("line", JString(l)) ::
      JField("engineeringAllowance", JInt(ea)) ::
      JField("engineeringAllowanceHalfMinute", JBool(eah)) ::
      JField("pathingAllowance", JInt(pa)) ::
      JField("pathingAllowanceHalfMinute", JBool(pah)) ::
      JField("performanceAllowance", JInt(pe)) ::
      JField("performanceAllowanceHalfMinute", JBool(peh)) ::
      JField("arrival", JInt(a)) ::
      JField("arrivalHalfMinute", JBool(ah)) ::
      JField("departure", JInt(d)) ::
      JField("departureHalfMinute", JBool(dh)) ::
      JField("path", JString(pth)) :: Nil
    ) => LocationIntermediate(t, p, l, ea.intValue(), eah, pa.intValue(), pah, pe.intValue(), peh, Some(a.intValue()), Some(ah), Some(d.intValue()), Some(dh), None, None, Some(pth), None, None)

  //terminal
  case JObject(
    JField("tiploc", JString(t)) ::
      JField("platform", JString(p)) ::
      JField("line", JString(l)) ::
      JField("engineeringAllowance", JInt(ea)) ::
      JField("engineeringAllowanceHalfMinute", JBool(eah)) ::
      JField("pathingAllowance", JInt(paa)) ::
      JField("pathingAllowanceHalfMinute", JBool(pah)) ::
      JField("performanceAllowance", JInt(pea)) ::
      JField("performanceAllowanceHalfMinute", JBool(peh)) ::
      JField("arrival", JInt(a)) ::
      JField("arrivalHalfMinute", JBool(ah)) ::
      JField("path", JString(pth)) ::
      JField("publicArrival", JInt(pa)) :: Nil
    ) => LocationTerminal(t, p, l, ea.intValue(), eah, pa.intValue(), pah, pea.intValue(), peh, Some(a.intValue()), Some(ah), Some(pth), Some(pa.intValue()))
}, {
  case _ => JString("")
}))

case object TransactionTypeSerializer extends CustomSerializer[TransactionType](format => ({
  case JString(transactionType) => transactionType match {
    case "New" => NewTransaction
    case "Delete" => DeleteTransaction
    case "Revise" => ReviseTransaction
  }
}, {
  case NewTransaction => JString("New")
  case DeleteTransaction => JString("Delete")
  case ReviseTransaction => JString("Revise")
}))

case object CategorySerializer extends CustomSerializer[Category](format => ({
  case JString(category) => category match {
    case "Join" => Join
    case "Divide" => Divide
    case "Next" => Next
    case "NoCategory" => NoCategory
  }
}, {
  case Join => JString("Join")
  case Divide => JString("Divide")
  case Next => JString("Next")
  case NoCategory => JString("NoCategory")
}))

case object DateIndicatorSerializer extends CustomSerializer[DateIndicator](format => ({
  case JString(dateIndicator) => dateIndicator match {
    case "Standard" => StandardDaytimeOnly
    case "OverNext" => OverNextMidnight
    case "OverPrevious" => OverPreviousMidnight
    case "None" => NoDateIndicator
  }
}, {
  case StandardDaytimeOnly => JString("Standard")
  case OverNextMidnight => JString("OverNext")
  case OverPreviousMidnight => JString("OverPrevious")
  case NoDateIndicator => JString("None")

}))

case object AssociationTypeSerializer extends CustomSerializer[AssociationType](format => ({
  case JString(assoc) => assoc match {
    case "Passenger" => Passenger
    case "Operating" => Operating
    case "None" => NoAssociationType
  }
}, {
  case Passenger => JString("Passenger")
  case Operating => JString("Operating")
  case NoAssociationType => JString("None")
}))

case object StpIndicatorSerializer extends CustomSerializer[StpIndicator](format => ({
  case JString(stp) => stp match {
    case "Cancellation" => Cancellation
    case "New" => New
    case "Overlay" => Overlay
    case "Permanent" => Permanent
  }
}, {
  case Cancellation => JString("Cancellation")
  case New => JString("New")
  case Overlay => JString("Overlay")
  case Permanent => JString("Permanent")
}))

case object BankHolidayRunningSerializer extends CustomSerializer[BankHolidayRunning](format => ({
  case JString(running) => running match {
    case "NotOnSpecifiedHolidays" => NotOnSpecifiedBankHolidayMondays
    case "NotOnGlasgowHolidays" => NotOnGlasgowBankHolidays
    case "RunsOnBankHolidays" => RunsOnBankHolidays
  }
}, {
  case NotOnSpecifiedBankHolidayMondays => JString("NotOnSpecifiedHolidays")
  case NotOnGlasgowBankHolidays => JString("NotOnGlasgowHolidays")
  case RunsOnBankHolidays => JString("RunsOnBankHolidays")
}))

case object TrainStatusSerializer extends CustomSerializer[TrainStatus](format => ({
  case JString(status) => status match {
    case "BusPermanent" => BusPermanent
    case "FreightPermanent" => FreightPermanent
    case "PassengerAndParcelsPermanent" => PassengerAndParcelsPermanent
    case "ShipPermanent" => ShipPermanent
    case "TripPermanent" => TripPermanent
    case "PassengerAndParcelsStp" => PassengerAndParcelsStp
    case "FreightStp" => FreightStp
    case "TripStp" => TripStp
    case "ShipStp" => ShipStp
    case "BusStp" => BusStp
  }
}, {
  case BusPermanent => JString("BusPermanent")
  case FreightPermanent => JString("FreightPermanent")
  case PassengerAndParcelsPermanent => JString("PassengerAndParcelsPermanent")
  case ShipPermanent => JString("ShipPermanent")
  case TripPermanent => JString("TripPermanent")
  case PassengerAndParcelsStp => JString("PassengerAndParcelsStp")
  case FreightStp => JString("FreightStp")
  case TripStp => JString("TripStp")
  case ShipStp => JString("ShipStp")
  case BusStp => JString("BusStp")
}))

case object TrainCategorySerializer extends CustomSerializer[TrainCategory](format => ({
  case JString(category) => category match {
    case "OrdinaryPassenger" => OrdinaryPassenger
    case "UnadvertisedOrdinaryPassenger" => UnadvertisedOrdinaryPassenger
    case "OrdinaryStaffTrain" => OrdinaryStaffTrain
    case "OrdinaryMixed" => OrdinaryMixed
    case "ExpressChannelTunnel" => ExpressChannelTunnel
    case "ExpressSleeperEuropeNightServices" => ExpressSleeperEuropeNightServices
    case "ExpressInternational" => ExpressInternational
    case "UnadvertisedExpress" => UnadvertisedExpress
    case "ExpressPassenger" => ExpressPassenger
    case "ExpressSleeperDomestic" => ExpressSleeperDomestic
    case "BusReplacement" => BusReplacement
    case "BusWtt" => BusWtt
    case "Ship" => Ship
    case "EmptyCoachingStock" => EmptyCoachingStock
    case "EmptyCoachingStockLondonUndergroundMetroService" => EmptyCoachingStockLondonUndergroundMetroService
    case "EmptyCoachingStockStaff" => EmptyCoachingStockStaff
    case "Postal" => Postal
    case "PostOfficeControlledParcels" => PostOfficeControlledParcels
    case "Parcels" => Parcels
    case "EmptyParcels" => EmptyParcels
    case "Departmental" => Departmental
    case "CivilEngineer" => CivilEngineer
    case "MechanicalAndElectricalEngineer" => MechanicalAndElectricalEngineer
    case "Stores" => Stores
    case "Test" => Test
    case "SignalAndTelecommunicationsEngineer" => SignalAndTelecommunicationsEngineer
    case "LocomotiveAndBrakeVan" => LocomotiveAndBrakeVan
    case "Locomotive" => Locomotive
    case "RFDAutomotiveComponents" => RFDAutomotiveComponents
    case "RFDAutomotiveVehicles" => RFDAutomotiveVehicles
    case "RFDEdibleProductsUKContracts" => RFDEdibleProductsUKContracts
    case "RFDIndustrialMineralsUKContracts" => RFDIndustrialMineralsUKContracts
    case "RFDChemicalsUkContracts" => RFDChemicalsUkContracts
    case "RFDBuildingMaterialsUKContracts" => RFDBuildingMaterialsUKContracts
    case "RFDGeneralMerchandiseUKContracts" => RFDGeneralMerchandiseUKContracts
    case "RFDEuropean" => RFDEuropean
    case "RFDFreightlinerContracts" => RFDFreightlinerContracts
    case "RFDFreightlinerOther" => RFDFreightlinerOther
    case "CoalDistributive" => CoalDistributive
    case "CoalElectricity" => CoalElectricity
    case "CoalOtherAndNuclear" => CoalOtherAndNuclear
    case "Metals" => Metals
    case "Aggregates" => Aggregates
    case "DomesticAndIndustrialWaste" => DomesticAndIndustrialWaste
    case "BuildingMaterials" => BuildingMaterials
    case "PetroleumProducts" => PetroleumProducts
    case "RFDEuropeanChannelTunnelMixedBusiness" => RFDEuropeanChannelTunnelMixedBusiness
    case "RFDEuropeanChannelTunnelIntermodal" => RFDEuropeanChannelTunnelIntermodal
    case "RFDEuropeanChannelTunnelAutomotive" => RFDEuropeanChannelTunnelAutomotive
    case "RFDEuropeanChannelTunnelContractServices" => RFDEuropeanChannelTunnelContractServices
    case "RFDEuropeanChannelTunnelHaulmark" => RFDEuropeanChannelTunnelHaulmark
    case "RFDEuropeanChannelTunnelJointVenture" => RFDEuropeanChannelTunnelJointVenture
    case value => OtherTrainCategory(value)
  }
}, {
  case OrdinaryPassenger => JString("OrdinaryPassenger")
  case UnadvertisedOrdinaryPassenger => JString("UnadvertisedOrdinaryPassenger")
  case OrdinaryStaffTrain => JString("OrdinaryStaffTrain")
  case OrdinaryMixed => JString("OrdinaryMixed")
  case ExpressChannelTunnel => JString("ExpressChannelTunnel")
  case ExpressSleeperEuropeNightServices => JString("ExpressSleeperEuropeNightServices")
  case ExpressInternational => JString("ExpressInternational")
  case UnadvertisedExpress => JString("UnadvertisedExpress")
  case ExpressPassenger => JString("ExpressPassenger")
  case ExpressSleeperDomestic => JString("ExpressSleeperDomestic")
  case BusReplacement => JString("BusReplacement")
  case BusWtt => JString("BusWtt")
  case Ship => JString("Ship")
  case EmptyCoachingStock => JString("EmptyCoachingStock")
  case EmptyCoachingStockLondonUndergroundMetroService => JString("EmptyCoachingStockLondonUndergroundMetroService")
  case EmptyCoachingStockStaff => JString("EmptyCoachingStockStaff")
  case Postal => JString("Postal")
  case PostOfficeControlledParcels => JString("PostOfficeControlledParcels")
  case Parcels => JString("Parcels")
  case EmptyParcels => JString("EmptyParcels")
  case Departmental => JString("Departmental")
  case CivilEngineer => JString("CivilEngineer")
  case MechanicalAndElectricalEngineer => JString("MechanicalAndElectricalEngineer")
  case Stores => JString("Stores")
  case Test => JString("Test")
  case SignalAndTelecommunicationsEngineer => JString("SignalAndTelecommunicationsEngineer")
  case LocomotiveAndBrakeVan => JString("LocomotiveAndBrakeVan")
  case Locomotive => JString("Locomotive")
  case RFDAutomotiveComponents => JString("RFDAutomotiveComponents")
  case RFDAutomotiveVehicles => JString("RFDAutomotiveVehicles")
  case RFDEdibleProductsUKContracts => JString("RFDEdibleProductsUKContracts")
  case RFDIndustrialMineralsUKContracts => JString("RFDIndustrialMineralsUKContracts")
  case RFDChemicalsUkContracts => JString("RFDChemicalsUkContracts")
  case RFDBuildingMaterialsUKContracts => JString("RFDBuildingMaterialsUKContracts")
  case RFDGeneralMerchandiseUKContracts => JString("RFDGeneralMerchandiseUKContracts")
  case RFDEuropean => JString("RFDEuropean")
  case RFDFreightlinerContracts => JString("RFDFreightlinerContracts")
  case RFDFreightlinerOther => JString("RFDFreightlinerOther")
  case CoalDistributive => JString("CoalDistributive")
  case CoalElectricity => JString("CoalElectricity")
  case CoalOtherAndNuclear => JString("CoalOtherAndNuclear")
  case Metals => JString("Metals")
  case Aggregates => JString("Aggregates")
  case DomesticAndIndustrialWaste => JString("DomesticAndIndustrialWaste")
  case BuildingMaterials => JString("BuildingMaterials")
  case PetroleumProducts => JString("PetroleumProducts")
  case RFDEuropeanChannelTunnelMixedBusiness => JString("RFDEuropeanChannelTunnelMixedBusiness")
  case RFDEuropeanChannelTunnelIntermodal => JString("RFDEuropeanChannelTunnelIntermodal")
  case RFDEuropeanChannelTunnelAutomotive => JString("RFDEuropeanChannelTunnelAutomotive")
  case RFDEuropeanChannelTunnelContractServices => JString("RFDEuropeanChannelTunnelContractServices")
  case RFDEuropeanChannelTunnelHaulmark => JString("RFDEuropeanChannelTunnelHaulmark")
  case RFDEuropeanChannelTunnelJointVenture => JString("RFDEuropeanChannelTunnelJointVenture")
  case OtherTrainCategory(value) => JString(value)
}))

case object PowerTypeSerializer extends CustomSerializer[PowerType](format => ({
  case JString(power) => power match {
    case "D" => Diesel
    case "DEMU" => DieselElectricMultipleUnit
    case "DMMU" => DieselMechanicalMultipleUnit
    case "E" => Electric
    case "ED" => ElectroDiesel
    case "EML" => ElectricMultipleUnitPlusLocomotive
    case "EMU" => ElectricMultipleUnit
    case "HST" => HighSpeedTrain
    case _ => NoTrainPowerType
  }
}, {
  case Diesel => JString("D")
  case DieselElectricMultipleUnit => JString("DEMU")
  case DieselMechanicalMultipleUnit => JString("DMMU")
  case Electric => JString("E")
  case ElectroDiesel => JString("ED")
  case ElectricMultipleUnitPlusLocomotive => JString("EML")
  case ElectricMultipleUnit => JString("EMU")
  case HighSpeedTrain => JString("HST")
  case NoTrainPowerType => JString("NA")
}))

case object TimingSerializer extends CustomSerializer[Timing](format => ({
  case JString(timing) => timing match {
    case "DMUPowerCarAndTrailer" => DMUPowerCarAndTrailer
    case "DMUTwoPowerCarAndTrailer" => DMUTwoPowerCarsAndTrailer
    case "DMUPowerTwin" => DMUPowerTwin
    case "EmuAcceleratedTimings" => EmuAcceleratedTimings
    case "Parcels" => ElectricParcelsUnit
    case "69" => Class("172/0", "172/1", "172/2")
    case "A" => Class("141", "142", "143", "144")
    case "E" => Class("158", "168", "170", "175", "EMU 458")
    case "N" => Class("165/0")
    case "S" => Class("150", "153", "155", "156")
    case "T" => Class("165/1", "166")
    case "V" => Class("220", "221")
    case "X" => Class("159")
    case "0" => Class("380")
    case "506" => Class("350/1 (110mph)")
    case _ => NoTiming
  }
  case JInt(int) if int >= 0 && int <= 999 => Class(int.toString)
  case JInt(int) if int >= 1000 && int <= 9999 => Hauled(int.intValue())
  case JObject(
    JField("load", JInt(load)) :: Nil
    ) => Hauled(load.intValue())

}, {
  case DMUPowerCarAndTrailer => JString("DMUPowerCarAndTrailer")
  case DMUTwoPowerCarsAndTrailer => JString("DMUTwoPowerCarAndTrailer")
  case DMUPowerTwin => JString("DMUPowerTwin")
  case EmuAcceleratedTimings => JString("EmuAcceleratedTimings")
  case ElectricParcelsUnit => JString("Parcels")
  case NoTiming => JString("NoTiming")

  case Class("172/0", "172/1", "172/2") => JString("69")
  case Class("172/0", "172/1", "172/2") => JString("69")
  case Class("141", "142", "143", "144") => JString("A")
  case Class("158", "168", "170", "175", "EMU 458") => JString("E")
  case Class("165/0") => JString("N")
  case Class("150", "153", "155", "156") => JString("S")
  case Class("165/1", "166") => JString("T")
  case Class("220", "221") => JString("V")
  case Class("159") => JString("X")
  case Class("380") => JString("0")
  case Class("350/1 (110mph)") => JString("506")

  case Class(classes) => JInt(classes.toInt)
}))

case object OperatingCharacteristicsSerializer extends CustomSerializer[OperatingCharacteristics](format => ({
  case JString(characteristcs) => characteristcs match {
    case "VacuumBraked" => VacuumBraked
    case "TimedAt100Mph" => TimedAt100Mph
    case "DriverOnlyOperatedCoachingStock" => DriverOnlyOperatedCoachingStock
    case "ConveysMark4Coaches" => ConveysMark4Coaches
    case "TrainmanGuardRequired" => TrainmanGuardRequired
    case "TimedAt110Mph" => TimedAt110Mph
    case "PushPullTrain" => PushPullTrain
    case "RunsAsRequired" => RunsAsRequired
    case "AirConditionedWithPaSystem" => AirConditionedWithPaSystem
    case "SteamHauled" => SteamHauled
    case "RunsToTerminalsYardsAsRequired" => RunsToTerminalsYardsAsRequired
    case "SB1CGaugeNoDiversionWithoutAuthority" => SB1CGaugeNoDiversionWithoutAuthority
    case "NoOperatingCharacteristics" => NoOperatingCharacteristics
  }
}, {
  case VacuumBraked => JString("VacuumBraked")
  case TimedAt100Mph => JString("TimedAt100Mph")
  case DriverOnlyOperatedCoachingStock => JString("DriverOnlyOperatedCoachingStock")
  case ConveysMark4Coaches => JString("ConveysMark4Coaches")
  case TrainmanGuardRequired => JString("TrainmanGuardRequired")
  case TimedAt110Mph => JString("TimedAt110Mph")
  case PushPullTrain => JString("PushPullTrain")
  case RunsAsRequired => JString("RunsAsRequired")
  case AirConditionedWithPaSystem => JString("AirConditionedWithPaSystem")
  case SteamHauled => JString("SteamHauled")
  case RunsToTerminalsYardsAsRequired => JString("RunsToTerminalsYardsAsRequired")
  case SB1CGaugeNoDiversionWithoutAuthority => JString("SB1CGaugeNoDiversionWithoutAuthority")
  case NoOperatingCharacteristics => JString("NoOperatingCharacteristics")
}))

case object SeatingSerializer extends CustomSerializer[Seating](format => ({
  case JString(seating) => seating match {
    case "FirstAndStandard" => FirstAndStandardSeating
    case "StandardOnly" => StandardOnlySeating
  }
}, {
  case StandardOnlySeating => JString("StandardOnly")
  case FirstAndStandardSeating => JString("FirstAndStandard")
}))

case object SleepersSerializer extends CustomSerializer[Sleepers](format => ({
  case JString(sleeper) => sleeper match {
    case "FirstAndStandard" => FirstAndStandardClassSleeper
    case "FirstOnly" => FirstClassOnlySleeper
    case "StandardOnly" => StandardClassOnlySleeper
    case _ => NoSleeper
  }
}, {
  case FirstAndStandardClassSleeper => JString("FirstAndStandard")
  case FirstClassOnlySleeper => JString("FirstOnly")
  case StandardClassOnlySleeper => JString("StandardOnly")
  case NoSleeper => JString("NoSleeper")
}))

case object ReservationsSerializer extends CustomSerializer[Reservations](format => ({
  case JString(reservations) => reservations match {
    case "Compulsory" => ReservationsCompulsory
    case "Bicycles" => ReservationsForBicycles
    case "Recommended" => ReservationsRecommended
    case "AnyStation" => ReservationsPossibleFromAnyStation
    case _ => NoReservations
  }
}, {
  case ReservationsCompulsory => JString("Compulsory")
  case ReservationsForBicycles => JString("Bicycles")
  case ReservationsRecommended => JString("Recommended")
  case ReservationsPossibleFromAnyStation => JString("AnyStation")
  case NoReservations => JString("NoReservations")
}))

case object CateringSerializer extends CustomSerializer[Catering](format => ({
  case JString(catering) => catering match {
    case "Buffet" => BuffetServiceCatering
    case "FirstClassRestaurant" => RestaurantCarFirstClassCatering
    case "HotFood" => HotFoodCatering
    case "MealIncluded" => MealIncludedFirstClassCatering
    case "WheelchairOnly" => WheelchairOnlyReservationsCatering
    case "Restaurant" => RestaurantCatering
    case "TrolleyService" => TrolleyServiceCatering
    case _ => NoCatering
  }
}, {
  case BuffetServiceCatering => JString("Buffet")
  case RestaurantCarFirstClassCatering => JString("FirstClassRestaurant")
  case HotFoodCatering => JString("HotFood")
  case MealIncludedFirstClassCatering => JString("MealIncluded")
  case WheelchairOnlyReservationsCatering => JString("WheelchairOnly")
  case RestaurantCatering => JString("Restaurant")
  case TrolleyServiceCatering => JString("TrolleyService")
  case NoCatering => JString("NoCatering")
}))

case object TimetableCodeSerializer extends CustomSerializer[TimetableCode](format => ({
  case JString(tc) => tc match {
    case "Y" => SubjectToMonitoring
    case "N" => NotSubjectToMonitoring
  }
}, {
  case SubjectToMonitoring => JString("Y")
  case NotSubjectToMonitoring => JString("N")
}))