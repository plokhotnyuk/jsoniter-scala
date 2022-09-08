package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Setup
import java.nio.charset.StandardCharsets.UTF_8

abstract class MissingRequiredFieldsBenchmark extends CommonParams {
  var jsonBytes: Array[Byte] = _
  var jsonString: String = "{}"

  @Setup
  def setup(): Unit = {
    jsonBytes = jsonString.getBytes(UTF_8)
  }
}