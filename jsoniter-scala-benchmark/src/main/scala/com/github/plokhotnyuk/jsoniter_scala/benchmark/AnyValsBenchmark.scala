package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.transparent
import com.fasterxml.jackson.annotation.JsonValue
import pl.iterators.kebs.json.noflat

import scala.annotation.meta.getter

@transparent case class ByteVal(@(JsonValue @getter) a: Byte) extends AnyVal

@transparent case class ShortVal(@(JsonValue @getter) a: Short) extends AnyVal

@transparent case class IntVal(@(JsonValue @getter) a: Int) extends AnyVal

@transparent case class LongVal(@(JsonValue @getter) a: Long) extends AnyVal

@transparent case class BooleanVal(@(JsonValue @getter) a: Boolean) extends AnyVal

@transparent case class CharVal(@(JsonValue @getter) a: Char) extends AnyVal

@transparent case class DoubleVal(@(JsonValue @getter) a: Double) extends AnyVal

@transparent case class FloatVal(@(JsonValue @getter) a: Float) extends AnyVal

@noflat case class AnyVals(b: ByteVal, s: ShortVal, i: IntVal, l: LongVal, bl: BooleanVal, ch: CharVal, dbl: DoubleVal, f: FloatVal)

abstract class AnyValsBenchmark extends CommonParams {
  //FIXME: 2.5 is for hiding of Play-JSON bug in serialization of floats as doubles: 2.2 -> 2.200000047683716
  var obj: AnyVals = AnyVals(ByteVal(1), ShortVal(2), IntVal(3), LongVal(4), BooleanVal(true), CharVal('x'), DoubleVal(1.1), FloatVal(2.5f))
  var jsonString1: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":1.1,"f":2.5}"""
  var jsonString2: String = """{"b":1,"bl":true,"ch":"x","dbl":1.1,"f":2.5,"i":3,"l":4,"s":2}"""
  var jsonBytes: Array[Byte] = jsonString1.getBytes(UTF_8)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
}