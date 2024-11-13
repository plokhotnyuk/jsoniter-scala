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