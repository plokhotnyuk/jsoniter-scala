package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.MonthDay
import org.openjdk.jmh.annotations.{Param, Setup}

abstract class ArrayOfMonthDaysBenchmark extends CommonParams {
  var obj: Array[MonthDay] = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString: String = _
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 512

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => MonthDay.of((i % 12) + 1, (i % 29) + 1)).toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 128/*to avoid possible out-of-bounds error*/)
  }
}