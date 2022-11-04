package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.fasterxml.jackson.annotation.JsonValue
import scala.annotation.meta.getter

case class ByteVal(@(JsonValue @getter) a: Byte) extends AnyVal

case class ShortVal(@(JsonValue @getter) a: Short) extends AnyVal

case class IntVal(@(JsonValue @getter) a: Int) extends AnyVal

case class LongVal(@(JsonValue @getter) a: Long) extends AnyVal

case class BooleanVal(@(JsonValue @getter) a: Boolean) extends AnyVal

case class CharVal(@(JsonValue @getter) a: Char) extends AnyVal

case class DoubleVal(@(JsonValue @getter) a: Double) extends AnyVal

case class FloatVal(@(JsonValue @getter) a: Float) extends AnyVal

case class AnyVals(b: ByteVal, s: ShortVal, i: IntVal, l: LongVal, bl: BooleanVal, ch: CharVal, dbl: DoubleVal, f: FloatVal)
