package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.transparent
import com.fasterxml.jackson.annotation.JsonValue
import scala.annotation.meta.getter

@transparent case class ByteVal(@(JsonValue @getter) a: Byte) extends AnyVal

@transparent case class ShortVal(@(JsonValue @getter) a: Short) extends AnyVal

@transparent case class IntVal(@(JsonValue @getter) a: Int) extends AnyVal

@transparent case class LongVal(@(JsonValue @getter) a: Long) extends AnyVal

@transparent case class BooleanVal(@(JsonValue @getter) a: Boolean) extends AnyVal

@transparent case class CharVal(@(JsonValue @getter) a: Char) extends AnyVal

@transparent case class DoubleVal(@(JsonValue @getter) a: Double) extends AnyVal

@transparent case class FloatVal(@(JsonValue @getter) a: Float) extends AnyVal

case class AnyVals(b: ByteVal, s: ShortVal, i: IntVal, l: LongVal, bl: BooleanVal, ch: CharVal, dbl: DoubleVal, f: FloatVal)

abstract class AnyValsBenchmark extends CommonParams {
  var obj: AnyVals = AnyVals(ByteVal(1), ShortVal(2), IntVal(3), LongVal(4), BooleanVal(true), CharVal('x'), DoubleVal(5), FloatVal(6))
  var jsonString1: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":5.0,"f":6.0}"""
  var jsonString2: String = """{"b":1,"bl":true,"ch":"x","dbl":5.0,"f":6.0,"i":3,"l":4,"s":2}"""
  var jsonString3: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":5,"f":6}"""
  var jsonBytes: Array[Byte] = jsonString1.getBytes(UTF_8)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
}