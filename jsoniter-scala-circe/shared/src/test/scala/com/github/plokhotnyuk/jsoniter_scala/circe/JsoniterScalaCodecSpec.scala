/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.circe

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec._
import io.circe.{Json, JsonNumber}
import io.circe.parser._

class JsoniterScalaCodecSpec extends AnyWordSpec with Matchers {
  "JsoniterScalaCodec.jsonCodec" should {
    "deserialize json" in {
      val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"b":true,"a":[null,"WWW",[],{},false],"o":{"a":[]}}"""
      val json = parse(jsonStr).getOrElse(null)
      readFromString(jsonStr) shouldBe json
    }
    "not deserialize invalid json" in {
      assert(intercept[JsonReaderException](readFromString("""{"n":null[]""")).getMessage.contains("expected '}' or ','"))
      assert(intercept[JsonReaderException](readFromString("""[null{}""")).getMessage.contains("expected ']' or ','"))
    }
    "not deserialize deeply nested json" in {
      val jsonStr1 = """[{"n":""" * 64 + "[]" + "}]" * 64
      val jsonStr2 = """{"n":[""" * 64 + "{}" + "]}" * 64
      assert(intercept[JsonReaderException](readFromString(jsonStr1)).getMessage.contains("depth limit exceeded"))
      assert(intercept[JsonReaderException](readFromString(jsonStr2)).getMessage.contains("depth limit exceeded"))
    }
    "allow customization for number parsing" in {
      val codec = jsonCodec(numberParser = in => Json.fromDoubleOrNull(in.readDouble())) // compatible with JS and faster than the default one
      val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val json = parse(jsonStr).getOrElse(null)
      readFromString(jsonStr)(codec) shouldBe json
    }
    "serialize json" in {
      val jsonStr = """{"s":"VVV","n1":1.0,"n2":2,"b":true,"a":[null,"WWW",[],{},false],"o":{"a":[]}}"""
      val json = readFromString(jsonStr)
      writeToString(json) shouldBe jsonStr
    }
    "allow customization for number serialization" in {
      val codec = jsonCodec(numberSerializer = io.circe.JsoniterScalaCodec.jsCompatibleNumberSerializer)
      val jsonStr = """{"s":1,"n1":1.0,"n2":4503599627370497,"a":[null,"WWW",[],{}],"o":{"a":4e+297}}"""
      val json = readFromString(jsonStr)
      writeToString(json)(codec) shouldBe """{"s":1,"n1":1.0,"n2":"4503599627370497","a":[null,"WWW",[],{}],"o":{"a":"4E+297"}}"""
      writeToString(Json.fromFloatOrNull(3.1415927f))(codec) shouldBe "3.1415927"
      writeToString(Json.fromDoubleOrNull(3.141592653589793))(codec) shouldBe "3.141592653589793"
      writeToString(Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("3.141592653589793")))(codec) shouldBe "3.141592653589793"
      writeToString(Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("3.14159265358979323846264338327950288419716939937510")))(codec) shouldBe """"3.14159265358979323846264338327950288419716939937510""""
      writeToString(Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("4e+297")))(codec) shouldBe """"4E+297""""
    }
    "allow customization for key sorting during serialization" in {
      val codec = jsonCodec(sortKeys = true)
      val jsonStr = """{"s":1,"n1":1.0,"n2":4503599627370497,"a":[null,"WWW",[],{}],"o":{"a":4e+297}}"""
      val json = readFromString(jsonStr)
      writeToString(json)(codec) shouldBe """{"a":[null,"WWW",[],{}],"n1":1.0,"n2":4503599627370497,"o":{"a":4E+297},"s":1}"""
    }
    "not serialize invalid json" in {
      val json1 = parse("\"\ud800\"").getOrElse(null)
      assert(intercept[Throwable](writeToString(json1)).getMessage.contains("illegal char sequence of surrogate pair"))
    }
    "not serialize deeply nested json" in {
      val json1 = parse("""[{"n":""" * 64 + "[]" + "}]" * 64).getOrElse(null)
      val json2 = parse("""{"n":[""" * 64 + "{}" + "]}" * 64).getOrElse(null)
      assert(intercept[Throwable](writeToString(json1)).getMessage.contains("depth limit exceeded"))
      assert(intercept[Throwable](writeToString(json2)).getMessage.contains("depth limit exceeded"))
    }
    "allow filtering for key-value serialization" in {
      val codec = jsonCodec(doSerialize = _ ne Json.Null)
      val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val jsonStrExpected = """{"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val json = readFromString(jsonStr)(codec)
      writeToString(json)(codec) shouldBe jsonStrExpected
    }
    "deserialize negative zeros keeping the sign like circe does" in {
      Seq("-0", "-0.0", "-0e0", "-0.00E+5", "[-0]", "{\"n\":-0.0}").foreach { jsonStr =>
        readFromString(jsonStr) shouldBe parse(jsonStr).getOrElse(null)
      }
      Seq("-0", "-0.0", "-0e0").foreach { jsonStr =>
        (1.0 / readFromString(jsonStr).asNumber.get.toDouble) shouldBe Double.NegativeInfinity
      }
      Seq("0", "0.0", "-1", "-0.5", "-1e-5").foreach { jsonStr =>
        readFromString(jsonStr) shouldBe parse(jsonStr).getOrElse(null)
        (1.0 / readFromString(jsonStr).asNumber.get.toDouble) should not be Double.NegativeInfinity
      }
      writeToString(readFromString("-0")) shouldBe "-0"
      writeToString(readFromString("-0.0")) shouldBe "-0.0"
    }
    "deserialize decimal numbers exactly like circe does" in {
      val pi = "3.14159265358979323846264338327950288419716939937510"
      readFromString(pi) shouldBe Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(pi)) // circe-parser for Scala.js parses numbers to `Double`
      writeToString(readFromString(pi)) shouldBe pi
      val longDecimal = "0." + "1234567890" * 30 + "123456" // 307 digits, including the leading zero
      writeToString(readFromString(longDecimal)) shouldBe longDecimal
      readFromString(s"[$longDecimal]") shouldBe Json.arr(Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(longDecimal)))
      assert(intercept[JsonReaderException](readFromString(longDecimal + "8")).getMessage.contains("value exceeds limit for number of digits"))
      assert(intercept[JsonReaderException](readFromString("1e-6178")).getMessage.contains("value exceeds limit for scale"))
    }
    "allow customization of the math context and limits for number parsing" in {
      val roundingCodec = jsonCodec(numberParser = io.circe.JsoniterScalaCodec.numberParser(mc = JsonReader.bigDecimalMathContext))
      writeToString(readFromString("3.14159265358979323846264338327950288419716939937510")(roundingCodec)) shouldBe "3.141592653589793238462643383279503"
      writeToString(readFromString("[-0.0]")(roundingCodec)) shouldBe "[-0.0]"
      val limitingCodec = jsonCodec(numberParser = io.circe.JsoniterScalaCodec.numberParser(scaleLimit = 10, digitsLimit = 10))
      writeToString(readFromString("[1.23456789,123456789,1e-9]")(limitingCodec)) shouldBe "[1.23456789,123456789,1E-9]"
      assert(intercept[JsonReaderException](readFromString("1.234567890")(limitingCodec)).getMessage.contains("value exceeds limit for number of digits"))
      assert(intercept[JsonReaderException](readFromString("1234567890")(limitingCodec)).getMessage.contains("value exceeds limit for number of digits"))
      assert(intercept[JsonReaderException](readFromString("1e-10")(limitingCodec)).getMessage.contains("value exceeds limit for scale"))
    }
    "deserialize a single big number" in {
      val jsonStr = "9223372036854775808"
      readFromString(jsonStr) shouldBe Json.fromBigInt(BigInt("9223372036854775808"))
    }
    "deserialize big numbers" in {
      val jsonStr = "[9223372036854775807,-9223372036854775808,9223372036854775809,9999999999999999999]"
      readFromString(jsonStr) shouldBe
        Json.arr(
          Json.fromBigInt(BigInt("9223372036854775807")),
          Json.fromBigInt(BigInt("-9223372036854775808")),
          Json.fromBigInt(BigInt("9223372036854775809")),
          Json.fromBigInt(BigInt("9999999999999999999"))
        )
    }
    "serialize numbers" in {
      val json = Json.arr(
        Json.fromFloatOrNull(1.0f),
        Json.fromDoubleOrNull(2.0),
        Json.fromLong(3L),
        Json.fromBigDecimal(BigDecimal("777e+777")),
        Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("999e+999"))
      )
      writeToString(json) shouldBe "[1.0,2.0,3,7.77E+779,999e+999]"
    }
  }
}