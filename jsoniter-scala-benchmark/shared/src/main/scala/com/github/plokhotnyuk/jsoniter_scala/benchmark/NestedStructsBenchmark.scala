package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import org.openjdk.jmh.annotations.{Param, Setup}

abstract class NestedStructsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 128
  var obj: NestedStructs = _
  var jsonBytes: Array[Byte] = _
  var jsonString: String = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 until size).foldLeft(NestedStructs(None))((n, _) => NestedStructs(Some(n)))
    jsonString = "{" + "\"n\":{" * (size - 1) + "}" * (size - 1) + "}"
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}