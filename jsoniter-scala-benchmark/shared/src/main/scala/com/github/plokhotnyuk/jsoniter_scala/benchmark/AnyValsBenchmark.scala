package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import org.openjdk.jmh.annotations.Setup

abstract class AnyValsBenchmark extends CommonParams {
  var obj: AnyVals = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString1: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":5.0,"f":6.0}"""
  var jsonString2: String = """{"b":1,"bl":true,"ch":"x","dbl":5.0,"f":6.0,"i":3,"l":4,"s":2}"""
  var jsonString3: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":5,"f":6}"""

  @Setup
  def setup(): Unit = {
    obj = AnyVals(ByteVal(1), ShortVal(2), IntVal(3), LongVal(4), BooleanVal(true), CharVal('x'), DoubleVal(5), FloatVal(6))
    jsonBytes = jsonString1.getBytes(UTF_8)
    preallocatedBuf = new Array(jsonBytes.length + 128/*to avoid possible out-of-bounds error*/)
  }
}