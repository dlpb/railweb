package models.timetable.model.train

sealed trait TimetableCode

case object SubjectToMonitoring extends TimetableCode

case object NotSubjectToMonitoring extends TimetableCode

object TimetableCode {
  def apply(key: String) = key match {
    case "Y" => SubjectToMonitoring
    case "N" => NotSubjectToMonitoring
    case _ => NotSubjectToMonitoring
  }
}

case class BasicScheduleExtraDetails(
                                      atocCode: String)

