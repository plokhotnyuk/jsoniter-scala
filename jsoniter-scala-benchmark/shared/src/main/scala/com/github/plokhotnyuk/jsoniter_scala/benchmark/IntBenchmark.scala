package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Setup
import java.nio.charset.StandardCharsets.UTF_8

abstract class IntBenchmark extends CommonParams {
  var obj: Int = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString: String = "1234567890"

  @Setup
  def setup(): Unit = {
    obj = jsonString.toInt
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array(jsonBytes.length + 128/*to avoid possible out of bounds error*/)
  }
}