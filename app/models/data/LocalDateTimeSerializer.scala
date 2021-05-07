package models.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

case object LocalDateTimeSerializer extends CustomSerializer[LocalDateTime](format => ({
  case JString(time) =>
    LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}, {
  case time: LocalDateTime =>
    JString(time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
}))
