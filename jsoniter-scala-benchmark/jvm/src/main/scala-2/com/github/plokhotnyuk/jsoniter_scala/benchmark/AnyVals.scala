package com.github.plokhotnyuk.jsoniter_scala.benchmark

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
