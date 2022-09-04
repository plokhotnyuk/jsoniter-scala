package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.nio.charset.StandardCharsets.UTF_8

abstract class MissingRequiredFieldsBenchmark extends CommonParams {
  var jsonString: String = """{}"""
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)
}