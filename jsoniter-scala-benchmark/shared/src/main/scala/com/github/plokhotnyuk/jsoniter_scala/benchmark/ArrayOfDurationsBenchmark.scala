package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import org.openjdk.jmh.annotations.{Param, Setup}

abstract class ArrayOfDurationsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[Duration] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map { i =>
      val x = Math.abs((i * 6971258582664805397L) / Math.pow(10, i % 19).toLong)
      val y = Math.abs(i * Math.pow(10, i % 10)).toInt
      Duration.ofSeconds(x, y)
    }.toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}