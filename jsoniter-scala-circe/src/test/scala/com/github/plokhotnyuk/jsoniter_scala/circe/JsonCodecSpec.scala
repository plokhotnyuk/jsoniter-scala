package com.github.plokhotnyuk.jsoniter_scala.circe

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.Json
import io.circe.parser._

class JsonCodecSpec extends AnyWordSpec with Matchers {
  "JsonCodec.jsonCodec" should {
    "deserialize json" in {
      import com.github.plokhotnyuk.jsoniter_scala.circe.JsonCodec._

      val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val json = parse(jsonStr).getOrElse(null)
      readFromString(jsonStr) shouldBe json
    }
    "not deserialize deeply nested json" in {
      import com.github.plokhotnyuk.jsoniter_scala.circe.JsonCodec._

      val jsonStr = "[" + """{"n":[""" * 64 + "]}" * 64 + "]"
      assert(intercept[JsonReaderException](readFromString(jsonStr)).getMessage.contains("depth limit exceeded"))
    }
    "allow customization for number parsing" in {
      implicit val jsonCodec: JsonValueCodec[Json] =
        com.github.plokhotnyuk.jsoniter_scala.circe.JsonCodec.jsonCodec(numberParser = in => {
          var b: Byte = 0
          in.setMark()
          try {
            while ({
              b = in.nextByte()
              b >= '0' && b <= '9' || b == '-'
            }) ()
          } catch {
            case _: JsonReaderException => // ignore the end of input error for now
          } finally in.rollbackToMark()
          if (b == '.' || b == 'e' || b == 'E') Json.fromDoubleOrNull(in.readDouble())
          else Json.fromInt(in.readInt())
        })

      val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val json = parse(jsonStr).getOrElse(null)
      readFromString(jsonStr) shouldBe json
    }
    "serialize json" in {
      import com.github.plokhotnyuk.jsoniter_scala.circe.JsonCodec._

      val jsonStr = """{"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val json = readFromString(jsonStr)
      writeToString(json) shouldBe jsonStr
    }
    "not serialize deeply nested json" in {
      import com.github.plokhotnyuk.jsoniter_scala.circe.JsonCodec._

      val jsonStr = "[" + """{"n":[""" * 64 + "]}" * 64 + "]"
      val json = parse(jsonStr).getOrElse(null)
      assert(intercept[Throwable](writeToString(json)).getMessage.contains("depth limit exceeded"))
    }
    "allow filtering for key-value serialization" in {
      implicit val jsonCodec: JsonValueCodec[Json] =
        com.github.plokhotnyuk.jsoniter_scala.circe.JsonCodec.jsonCodec(doSerialize = _ ne Json.Null)

      val jsonStr = """{"n":null,"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val jsonStrExpected = """{"s":"VVV","n1":1.0,"n2":2,"a":[null,"WWW",[],{}],"o":{"a":[]}}"""
      val json = readFromString(jsonStr)
      writeToString(json) shouldBe jsonStrExpected
    }
  }
}
