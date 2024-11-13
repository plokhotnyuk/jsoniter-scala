package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Year
import org.openjdk.jmh.annotations.{Param, Setup}

abstract class ArrayOfYearsBenchmark extends CommonParams {
  var obj: Array[Year] = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString: String = _
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => Year.of(i % 1000 + 2000)).toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 128/*to avoid possible out-of-bounds error*/)
  }
}