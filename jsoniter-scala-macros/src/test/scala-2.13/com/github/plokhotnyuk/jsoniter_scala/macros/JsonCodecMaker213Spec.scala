package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make

case class LiteralTypes(s: "VVV", b: true, c: '1', i: 2, l: 3L, f: 4.0f, d: 5.0)

case class LiteralAnyVal(v: 7) extends AnyVal

class JsonCodecMaker213Spec extends VerifyingSpec {
  "JsonCodecMaker213.make generate codes which" should {
    "serialize and deserialize case class with literal types" in {
      verifySerDeser(make[LiteralTypes](CodecMakerConfig()),
        LiteralTypes("VVV", true, '1', 2, 3L, 4.0f, 5.0),
        """{"s":"VVV","b":true,"c":"1","i":2,"l":3,"f":4.0,"d":5.0}""")
    }
    "serialize and deserialize any val with a literal type" in {
      verifySerDeser(make[LiteralAnyVal](CodecMakerConfig()), LiteralAnyVal(7), "7")
    }
    "serialize and deserialize top-level literal types" in {
      verifySerDeser(make["VVV"](CodecMakerConfig()), "VVV", "\"VVV\"")
      verifySerDeser(make[true](CodecMakerConfig()), true, "true")
      verifySerDeser(make['1'](CodecMakerConfig()), '1', "\"1\"")
      verifySerDeser(make[2](CodecMakerConfig()), 2, "2")
      verifySerDeser(make[3L](CodecMakerConfig()), 3L, "3")
      verifySerDeser(make[4.0f](CodecMakerConfig()), 4.0f, "4.0")
      verifySerDeser(make[5.0](CodecMakerConfig()), 5.0, "5.0")
    }
    "serialize and deserialize stringifeid top-level literal types" in {
      verifySerDeser(make["VVV"](CodecMakerConfig(isStringified = true)), "VVV", "\"VVV\"")
      verifySerDeser(make[true](CodecMakerConfig(isStringified = true)), true, "\"true\"")
      verifySerDeser(make['1'](CodecMakerConfig(isStringified = true)), '1', "\"1\"")
      verifySerDeser(make[2](CodecMakerConfig(isStringified = true)), 2, "\"2\"")
      verifySerDeser(make[3L](CodecMakerConfig(isStringified = true)), 3L, "\"3\"")
      verifySerDeser(make[4.0f](CodecMakerConfig(isStringified = true)), 4.0f, "\"4.0\"")
      verifySerDeser(make[5.0](CodecMakerConfig(isStringified = true)), 5.0, "\"5.0\"")
    }
    "throw parse exception with expected value in case of illegal input" in {
      verifyDeserError(make["VVV"](CodecMakerConfig()), "\"XXX\"", "expected value: \"VVV\", offset: 0x00000004")
      verifyDeserError(make[true](CodecMakerConfig()), "false", "expected value: true, offset: 0x00000004")
      verifyDeserError(make['1'](CodecMakerConfig()), "\"0\"", "expected value: \"1\", offset: 0x00000002")
      verifyDeserError(make[2](CodecMakerConfig()), "0", "expected value: 2, offset: 0x00000000")
      verifyDeserError(make[3L](CodecMakerConfig()), "0", "expected value: 3, offset: 0x00000000")
      verifyDeserError(make[4.0f](CodecMakerConfig()), "0.0", "expected value: 4.0, offset: 0x00000002")
      verifyDeserError(make[5.0](CodecMakerConfig()), "0.0", "expected value: 5.0, offset: 0x00000002")
    }
    "serialize and deserialize literal types as keys" in {
      verifySerDeser(make[Map["VVV", Int]](CodecMakerConfig()), Map["VVV", Int](("VVV", 0)), "{\"VVV\":0}")
      verifySerDeser(make[Map[true, Int]](CodecMakerConfig()), Map[true, Int]((true, 0)), "{\"true\":0}")
      verifySerDeser(make[Map['1', Int]](CodecMakerConfig()), Map['1', Int](('1', 0)), "{\"1\":0}")
      verifySerDeser(make[Map[2, Int]](CodecMakerConfig()), Map[2, Int]((2, 0)), "{\"2\":0}")
      verifySerDeser(make[Map[3L, Int]](CodecMakerConfig()), Map[3L, Int]((3L, 0)), "{\"3\":0}")
      verifySerDeser(make[Map[4.0f, Int]](CodecMakerConfig()), Map[4.0f, Int]((4.0f, 0)), "{\"4.0\":0}")
      verifySerDeser(make[Map[5.0, Int]](CodecMakerConfig()), Map[5.0, Int]((5.0, 0)), "{\"5.0\":0}")
    }
    "throw parse exception with expected key in case of illegal input" in {
      verifyDeserError(make[Map["VVV", Int]](CodecMakerConfig()), "{\"XXX\":0}", "expected key: \"VVV\", offset: 0x00000006")
      verifyDeserError(make[Map[true, Int]](CodecMakerConfig()), "{\"false\":0}", "expected key: \"true\", offset: 0x00000008")
      verifyDeserError(make[Map['1', Int]](CodecMakerConfig()), "{\"0\":0}", "expected key: \"1\", offset: 0x00000004")
      verifyDeserError(make[Map[2, Int]](CodecMakerConfig()), "{\"0\":0}", "expected key: \"2\", offset: 0x00000004")
      verifyDeserError(make[Map[3L, Int]](CodecMakerConfig()), "{\"0\":0}", "expected key: \"3\", offset: 0x00000004")
      verifyDeserError(make[Map[4.0f, Int]](CodecMakerConfig()), "{\"0.0\":0}", "expected key: \"4.0\", offset: 0x00000006")
      verifyDeserError(make[Map[5.0, Int]](CodecMakerConfig()), "{\"0.0\":0}", "expected key: \"5.0\", offset: 0x00000006")
    }
  }
}
