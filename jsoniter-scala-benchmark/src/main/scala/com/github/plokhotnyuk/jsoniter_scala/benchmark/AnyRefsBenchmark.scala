package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

case class AnyRefs(s: String, bd: BigDecimal, os: Option[String] = None)

abstract class AnyRefsBenchmark extends CommonParams {
  var obj: AnyRefs = AnyRefs("s", 1, Some("os"))
  var jsonString1: String = """{"s":"s","bd":1,"os":"os"}"""
  var jsonString2: String = """{"bd":1,"os":"os","s":"s"}"""
  var jsonBytes: Array[Byte] = jsonString1.getBytes(UTF_8)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
}