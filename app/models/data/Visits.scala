package models.data

import java.time.LocalDateTime

import models.location.Location
import models.route.Route


sealed trait Visit[T] {
  def `type`: String = getClass.getName
  def version = 1
  def visited: T
  def created: LocalDateTime
  def eventOccurredAt: LocalDateTime
  def description: String
}

case class DataModelVisit(
                           override val visited: String,
                           override val created: LocalDateTime,
                           override val eventOccurredAt: LocalDateTime,
                           override val description: String
                           ) extends Visit[String]

case class LocationVisit (
                           override val visited: Location,
                           override val created: LocalDateTime,
                           override val eventOccurredAt: LocalDateTime,
                           override val description: String
                         ) extends Visit[Location]

case class RouteVisit ( override val visited: Route,
                        override val created: LocalDateTime,
                        override val eventOccurredAt: LocalDateTime,
                        override val description: String
                      ) extends Visit[Route]

