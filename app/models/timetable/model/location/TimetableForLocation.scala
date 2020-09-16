package models.timetable.model.location

import java.time.LocalTime

import models.timetable.model.location.TimetableForLocationTypes.TrainUid
import models.timetable.model.train.StpIndicator

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
                                 arr: Option[LocalTime],
                                 dep: Option[LocalTime],
                                 pass: Option[LocalTime],
                                 path: Option[String],
                                 platform: Option[String],
                                 pubArr: Option[LocalTime],
                                 pubDep: Option[LocalTime],
                                 stpIndicator: StpIndicator,
                                 toc: Option[String]
                               )

