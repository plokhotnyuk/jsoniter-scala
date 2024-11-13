package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats.stringFormat
import play.api.libs.json.Format
import java.time._

trait PlatformSpecificPlayJsonFormats {
  implicit val durationFormat: Format[Duration] = stringFormat("instant")(Duration.parse)
  implicit val instantFormat: Format[Instant] = stringFormat("instant")(Instant.parse)
  implicit val localDateTimeFormat: Format[LocalDateTime] = stringFormat("localdatetime")(LocalDateTime.parse)
  implicit val localDateFormat: Format[LocalDate] = stringFormat("localdate")(LocalDate.parse)
  implicit val localTimeFormat: Format[LocalTime] = stringFormat("localtime")(LocalTime.parse)
  implicit val offsetDateTimeFormat: Format[OffsetDateTime] = stringFormat("offsetdatetime")(OffsetDateTime.parse)
  implicit val zoneIdFormat: Format[ZoneId] = stringFormat("zoneid")(ZoneId.of)
  implicit val zonedDateTimeFormat: Format[ZonedDateTime] = stringFormat("zoneddatetime")(ZonedDateTime.parse)
  implicit val periodFormat: Format[Period] = stringFormat("period")(Period.parse)
}
