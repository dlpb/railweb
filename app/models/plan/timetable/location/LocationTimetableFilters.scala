package models.plan.timetable.location

import models.timetable.model.train.{BusReplacement, BusWtt, ExpressChannelTunnel, ExpressInternational, ExpressMotorail, ExpressPassenger, ExpressSleeperDomestic, ExpressSleeperEuropeNightServices, OrdinaryLondonUndergroundMetroService, OrdinaryPassenger, OrdinaryStaffTrain, Ship, TrainCategory, UnadvertisedExpress, UnadvertisedOrdinaryPassenger}

object LocationTimetableFilters {
  def isPublicCategory(category: TrainCategory) = {
    category.equals(OrdinaryLondonUndergroundMetroService) ||
      category.equals(UnadvertisedOrdinaryPassenger) ||
      category.equals(OrdinaryPassenger) ||
      category.equals(OrdinaryStaffTrain) ||
      category.equals(ExpressChannelTunnel) ||
      category.equals(ExpressSleeperEuropeNightServices) ||
      category.equals(ExpressInternational) ||
      category.equals(ExpressMotorail) ||
      category.equals(UnadvertisedExpress) ||
      category.equals(ExpressPassenger) ||
      category.equals(ExpressSleeperDomestic) ||
      category.equals(BusWtt) ||
      category.equals(BusReplacement) ||
      category.equals(Ship)
  }
}
