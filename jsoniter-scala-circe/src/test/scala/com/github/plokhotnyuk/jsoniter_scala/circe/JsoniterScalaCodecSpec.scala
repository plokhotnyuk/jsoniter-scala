package com.github.plokhotnyuk.jsoniter_scala.circe

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.Json
import io.circe.parser._

class JsoniterScalaCodecSpec extends AnyWordSpec with Matchers {
  "JsoniterScalaCodec.jsonCodec" should {
    "deserialize json" in {
      import com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec._

      val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val json = parse(jsonStr).getOrElse(null)
      readFromString(jsonStr) shouldBe json
    }
    "not deserialize deeply nested json" in {
      import com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec._

      val jsonStr = "[" + """{"n":[""" * 64 + "]}" * 64 + "]"
      assert(intercept[JsonReaderException](readFromString(jsonStr)).getMessage.contains("depth limit exceeded"))
    }
    "allow customization for number parsing" in {
      implicit val jsonCodec: JsonValueCodec[Json] =
        com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec
          .jsonCodec(numberParser = in => Json.fromDoubleOrNull(in.readDouble())) // compatible with JS and faster than default one

      val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val json = parse(jsonStr).getOrElse(null)
      readFromString(jsonStr) shouldBe json
    }
    "serialize json" in {
      import com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec._

      val jsonStr = """{"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val json = readFromString(jsonStr)
      writeToString(json) shouldBe jsonStr
    }
    "not serialize deeply nested json" in {
      import com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec._

      val jsonStr = "[" + """{"n":[""" * 64 + "]}" * 64 + "]"
      val json = parse(jsonStr).getOrElse(null)
      assert(intercept[Throwable](writeToString(json)).getMessage.contains("depth limit exceeded"))
    }
    "allow filtering for key-value serialization" in {
      implicit val jsonCodec: JsonValueCodec[Json] =
        com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec.jsonCodec(doSerialize = _ ne Json.Null)

      val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val jsonStrExpected = """{"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val json = readFromString(jsonStr)
      writeToString(json) shouldBe jsonStrExpected
    }
  }
}
