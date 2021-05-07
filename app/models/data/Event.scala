package models.data

import java.time.LocalDateTime
import java.util.UUID

case class Event(
                  id: String = UUID.randomUUID().toString,
                  name: String = "",
                  details: String = "",
                  created: LocalDateTime = LocalDateTime.now(),
                  startedAt: LocalDateTime = LocalDateTime.now(),
                  endedAt: LocalDateTime = LocalDateTime.now.plusDays(1)
                )

