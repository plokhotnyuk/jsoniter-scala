package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.{LocalDate, LocalDateTime, LocalTime}
import org.openjdk.jmh.annotations.{Param, Setup}

abstract class ArrayOfLocalDateTimesBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[LocalDateTime] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map { i =>
      val n = Math.abs(i * 1498724053)
      LocalDateTime.of(LocalDate.ofEpochDay(i),
        LocalTime.ofNanoOfDay(((n % 86000) | 0x1) * 1000000000L + (i % 4 match {
          case 0 => 0
          case 1 => ((n % 1000) | 0x1) * 1000000
          case 2 => ((n % 1000000) | 0x1) * 1000
          case 3 => (n | 0x1) % 1000000000
        })))
    }.toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}