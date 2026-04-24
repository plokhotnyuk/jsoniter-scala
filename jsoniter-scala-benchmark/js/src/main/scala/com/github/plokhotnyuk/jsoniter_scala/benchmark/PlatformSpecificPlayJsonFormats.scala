/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
