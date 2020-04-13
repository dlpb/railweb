package models.timetable.model.location

import java.util.Date

import models.timetable.model.location.TimetableForLocationTypes.{Platform, Time, Tiploc, TrainUid}
import models.timetable.model.train.{BankHolidayRunning, StpIndicator}

object TimetableForLocationTypes {
  type TrainUid = String
  type Tiploc = String
  type Platform = String
  type Time = String
}

case class TimetableForLocation(
                                 id: String,
                                 uid: TrainUid,
                                 publicTrain: Boolean,
                                 publicStop: Boolean,
                                 origin: Option[String],
                                 destination: Option[String],
                                 arr: Option[Time],
                                 dep: Option[Time],
                                 pass: Option[Time],
                                 path: Option[String],
                                 platform: Option[String],
                                 pubArr: Option[Time],
                                 pubDep: Option[Time],
                                 stpIndicator: StpIndicator,
                                 toc: Option[String]
                               )

