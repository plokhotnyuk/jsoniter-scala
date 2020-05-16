package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

class PrimitivesBenchmark extends CommonParams {
  var obj: Primitives = Primitives(1, 2, 3, 4, bl = true, ch = 'x', 5, 6)
  var jsonString1: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":5.0,"f":6.0}"""
  var jsonString2: String = """{"b":1,"bl":true,"ch":"x","dbl":5.0,"f":6.0,"i":3,"l":4,"s":2}"""
  var jsonString3: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":5,"f":6}"""
  var jsonBytes: Array[Byte] = jsonString1.getBytes(UTF_8)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
}