package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Setup
import java.nio.charset.StandardCharsets.UTF_8

abstract class ADTBenchmark extends CommonParams {
  var obj: ADTBase = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString1: String = """{"type":"Z","l":{"type":"X","a":1},"r":{"type":"Y","b":"VVV"}}"""
  var jsonString2: String = """{"l":{"a":1,"type":"X"},"r":{"b":"VVV","type":"Y"},"type":"Z"}"""

  @Setup
  def setup(): Unit = {
    obj = Z(X(1), Y("VVV"))
    jsonBytes = jsonString1.getBytes(UTF_8)
    preallocatedBuf = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}