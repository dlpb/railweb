package models.data

import java.time.LocalDateTime
import java.util.UUID

import models.location.Location
import models.route.Route


sealed trait Visit[T] {
  def id: String
  def visited: T
  def created: LocalDateTime
  def eventOccurredAt: LocalDateTime
  def trainUid: Option[String]
  def description: Option[String]
}

case class DataModelVisit(
                           override val visited: String,
                           override val created: LocalDateTime,
                           override val eventOccurredAt: LocalDateTime,
                           override val description: Option[String],
                           override val trainUid: Option[String],
                           override val id: String = UUID.randomUUID().toString
                           ) extends Visit[String]

case class LocationVisit (
                           override val visited: Location,
                           override val created: LocalDateTime,
                           override val eventOccurredAt: LocalDateTime,
                           override val description: Option[String],
                           override val trainUid: Option[String],
                           override val id: String = UUID.randomUUID().toString
                         ) extends Visit[Location]

case class RouteVisit ( override val visited: Route,
                        override val created: LocalDateTime,
                        override val eventOccurredAt: LocalDateTime,
                        override val description: Option[String],
                        override val trainUid: Option[String],
                        override val id: String = UUID.randomUUID().toString
                      ) extends Visit[Route]

