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

import java.nio.charset.StandardCharsets.UTF_8
import java.time.{ZoneId, _}
import org.openjdk.jmh.annotations.{Param, Setup}
import scala.jdk.CollectionConverters._

abstract class ArrayOfZonedDateTimesBenchmark extends CommonParams {
  var obj: Array[ZonedDateTime] = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString: String = _
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 512

  @Setup
  def setup(): Unit = {
    val zoneIds = (ZoneId.getAvailableZoneIds.asScala.take(100).map(ZoneId.of) ++
      (1 to 7).map(i => ZoneId.of(s"+0$i:00")) ++
      (1 to 7).map(i => ZoneId.of(s"UT+0$i:00")) ++
      (1 to 7).map(i => ZoneId.of(s"UTC+0$i:00")) ++
      (1 to 7).map(i => ZoneId.of(s"GMT+0$i:00"))).toArray
    obj = (1 to size).map { i =>
      val n = Math.abs(i * 1498724053)
      ZonedDateTime.of(LocalDateTime.of(LocalDate.ofEpochDay(i),
        LocalTime.ofNanoOfDay(((n % 86000) | 0x1) * 1000000000L + (i % 4 match {
          case 0 => 0
          case 1 => ((n % 1000) | 0x1) * 1000000
          case 2 => ((n % 1000000) | 0x1) * 1000
          case 3 => (n | 0x1) % 1000000000
        }))),
        zoneIds(i % zoneIds.length))
    }.toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 128/*to avoid possible out-of-bounds error*/)
  }
}