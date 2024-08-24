package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Setup
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

class PrimitivesBenchmark extends CommonParams {
  var obj: Primitives = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString1: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":5.0,"f":6.0}"""
  var jsonString2: String = """{"b":1,"bl":true,"ch":"x","dbl":5.0,"f":6.0,"i":3,"l":4,"s":2}"""
  var jsonString3: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":5,"f":6}"""

  @Setup
  def setup(): Unit = {
    obj = Primitives(1, 2, 3, 4, bl = true, ch = 'x', 5, 6)
    jsonBytes = writeToArray(obj)
    preallocatedBuf = new Array(jsonBytes.length + 128/*to avoid possible out-of-bounds error*/)
  }
}