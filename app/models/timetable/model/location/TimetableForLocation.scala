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
                               ) extends Ordered[TimetableForLocation] {
  override def compare(that: TimetableForLocation): Int = {
    (pubDep, pubArr, pass, dep, arr, that.pubDep, that.pubArr, that.pass, that.dep, that.arr) match {
      case (Some(a), _, _, _, _, Some(b), _, _, _, _) => a.compareTo(b)
      case (Some(a), _, _, _, _, _, Some(b), _, _, _) => a.compareTo(b)
      case (Some(a), _, _, _, _, _, _, Some(b), _, _) => a.compareTo(b)
      case (Some(a), _, _, _, _, _, _, _, Some(b), _) => a.compareTo(b)
      case (Some(a), _, _, _, _, _, _, _, _, Some(b)) => a.compareTo(b)
      case (_, Some(a), _, _, _, Some(b), _, _, _, _) => a.compareTo(b)
      case (_, Some(a), _, _, _, _, Some(b), _, _, _) => a.compareTo(b)
      case (_, Some(a), _, _, _, _, _, Some(b), _, _) => a.compareTo(b)
      case (_, Some(a), _, _, _, _, _, _, Some(b), _) => a.compareTo(b)
      case (_, Some(a), _, _, _, _, _, _, _, Some(b)) => a.compareTo(b)
      case (_, _, Some(a), _, _, Some(b), _, _, _, _) => a.compareTo(b)
      case (_, _, Some(a), _, _, _, Some(b), _, _, _) => a.compareTo(b)
      case (_, _, Some(a), _, _, _, _, Some(b), _, _) => a.compareTo(b)
      case (_, _, Some(a), _, _, _, _, _, Some(b), _) => a.compareTo(b)
      case (_, _, Some(a), _, _, _, _, _, _, Some(b)) => a.compareTo(b)
      case (_, _, _, Some(a), _, Some(b), _, _, _, _) => a.compareTo(b)
      case (_, _, _, Some(a), _, _, Some(b), _, _, _) => a.compareTo(b)
      case (_, _, _, Some(a), _, _, _, Some(b), _, _) => a.compareTo(b)
      case (_, _, _, Some(a), _, _, _, _, Some(b), _) => a.compareTo(b)
      case (_, _, _, Some(a), _, _, _, _, _, Some(b)) => a.compareTo(b)
      case (_, _, _, _, Some(a), Some(b), _, _, _, _) => a.compareTo(b)
      case (_, _, _, _, Some(a), _, Some(b), _, _, _) => a.compareTo(b)
      case (_, _, _, _, Some(a), _, _, Some(b), _, _) => a.compareTo(b)
      case (_, _, _, _, Some(a), _, _, _, Some(b), _) => a.compareTo(b)
      case (_, _, _, _, Some(a), _, _, _, _, Some(b)) => a.compareTo(b)
      case _ => 0
    }
  }
}

