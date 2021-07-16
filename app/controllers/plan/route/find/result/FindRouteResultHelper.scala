package controllers.plan.route.find.result

import java.net.URLDecoder

import models.location.Location
import models.route.Route
import play.api.mvc.Call


object FindRouteResultHelper {

  def mkTime(totalSeconds: Long, suffix: String = ""): String = {
    var remaining = totalSeconds
    val hours = if(remaining >= 3600) remaining / 3600 else 0
    remaining = remaining - (hours * 3600)
    val minutes = if(remaining >= 60) remaining / 60 else 0
    remaining = remaining - (minutes * 60)
    val seconds = remaining
    val hoursStr = if(hours > 0) s"${hours}h" else ""
    val minutesStr = s"${minutes}m"
    val secondsStr = s"$seconds"
    val formattedTime = s"$hoursStr$minutesStr$secondsStr$suffix"
    formattedTime
  }

  def extractBooleanFromData(data: Option[Map[String, Seq[String]]], fieldName: String) = {
    val followFixedLinks: Boolean = data
      .getOrElse(Map.empty)
      .getOrElse(fieldName, Seq.empty)
      .headOption.
      exists(_.toBoolean)
    followFixedLinks
  }

  def extractStringListFromData(data: Option[Map[String, Seq[String]]], fieldName: String) = {
    val locationsToRouteVia: List[String] = data
      .getOrElse(Map.empty)
      .getOrElse(fieldName, Seq.empty)
      .map(URLDecoder.decode(_, "utf-8"))
      .map(_.split("\n"))
      .flatMap(_.toList)
      .toList
    locationsToRouteVia
  }

  def extractInt(data: Option[Map[String, Seq[String]]], toStr: String) = {
    data
      .getOrElse(Map.empty)
      .getOrElse(toStr, Seq.empty)
      .headOption
      .flatMap(_.toIntOption)
      .map(_ + 1)
      .getOrElse(-1)
  }

  def extractString(data: Option[Map[String, Seq[String]]], fieldName: String) = {
    data
      .getOrElse(Map.empty)
      .getOrElse(fieldName, Seq.empty)
      .headOption
      .flatMap(s => if (s.trim.isBlank) Option.empty[String] else Some(s))
  }

}

case class Waypoint(id: String, name: String, isPublicStop: Boolean)

case class ResultViewModel(
                            locationsList: List[models.location.Location],
                            routeList: List[models.route.Route],
                            waypoints: List[controllers.plan.route.find.result.Waypoint],
                            followFreightLinks: Boolean,
                            followFixedLinks: Boolean,
                            followUnknownLinks: Boolean,
                            distance: Long,
                            time: String,
                            manuallyCreated: Boolean,
                            jsonCreated: Boolean,
                            timetableCreated: Boolean,
                            timetableTrainUid: Option[String],
                            date: Option[java.time.LocalDate],
                            timetableTrainOrigin: Option[models.location.Location],
                            timetableTrainDestination: Option[models.location.Location],
                            visitMode: String,
                            fromLocationIndex: Int,
                            toLocationIndex: Int,
                            includeNonPublicStops: Boolean,
                            overrideDateAndTimeOfVisit: Boolean,
                            overriddenStartDate: String,
                            overriddenStartTime: String,
                            overrideVisitDetails: Boolean,
                            overriddenVisitName: String,
                            overriddenTrainUid: String,
                            overriddenTrainHeadcode: String,
                            locationsToVisit: List[Location],
                            routesToVisit: List[Route],
                            visitCall: Call,
                            editCall: Call
                          )
