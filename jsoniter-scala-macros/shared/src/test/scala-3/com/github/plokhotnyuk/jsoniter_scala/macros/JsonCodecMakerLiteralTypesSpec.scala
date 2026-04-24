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

package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make

case class LiteralTypes(s: "VVV", b: true, c: '1', i: 2, l: 3L, f: 4.0f, d: 5.0)

case class LiteralAnyVal(v: 7) extends AnyVal

class JsonCodecMakerLiteralTypesSpec extends VerifyingSpec {
  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize case class with literal types" in {
      verifySerDeser(make[LiteralTypes], LiteralTypes("VVV", true, '1', 2, 3L, 4.0f, 5.0),
        """{"s":"VVV","b":true,"c":"1","i":2,"l":3,"f":4.0,"d":5.0}""")
    }
    "serialize and deserialize any val with a literal type" in {
      verifySerDeser(make[LiteralAnyVal], LiteralAnyVal(7), "7")
    }
    "serialize and deserialize top-level literal types" in {
      verifySerDeser(make["VVV"], "VVV", "\"VVV\"")
      verifySerDeser(make[true], true, "true")
      verifySerDeser(make['1'], '1', "\"1\"")
      verifySerDeser(make[2], 2, "2")
      verifySerDeser(make[3L], 3L, "3")
      verifySerDeser(make[4.0f], 4.0f, "4.0")
      verifySerDeser(make[5.0], 5.0, "5.0")
    }
    "serialize and deserialize stringifeid top-level literal types" in {
      verifySerDeser(make["VVV"](CodecMakerConfig.withIsStringified(true)), "VVV", "\"VVV\"")
      verifySerDeser(make[true](CodecMakerConfig.withIsStringified(true)), true, "\"true\"")
      verifySerDeser(make['1'](CodecMakerConfig.withIsStringified(true)), '1', "\"1\"")
      verifySerDeser(make[2](CodecMakerConfig.withIsStringified(true)), 2, "\"2\"")
      verifySerDeser(make[3L](CodecMakerConfig.withIsStringified(true)), 3L, "\"3\"")
      verifySerDeser(make[4.0f](CodecMakerConfig.withIsStringified(true)), 4.0f, "\"4.0\"")
      verifySerDeser(make[5.0](CodecMakerConfig.withIsStringified(true)), 5.0, "\"5.0\"")
    }
    "throw parse exception with expected value in case of illegal input" in {
      verifyDeserError(make["VVV"], "\"XXX\"", "expected value: \"VVV\", offset: 0x00000004")
      verifyDeserError(make[true], "false", "expected value: true, offset: 0x00000004")
      verifyDeserError(make['1'], "\"0\"", "expected value: \"1\", offset: 0x00000002")
      verifyDeserError(make[2], "0", "expected value: 2, offset: 0x00000000")
      verifyDeserError(make[3L], "0", "expected value: 3, offset: 0x00000000")
      verifyDeserError(make[4.0f], "0.0", "expected value: 4.0, offset: 0x00000002")
      verifyDeserError(make[5.0], "0.0", "expected value: 5.0, offset: 0x00000002")
    }
    "serialize and deserialize literal types as keys" in {
      verifySerDeser(make[Map["VVV", Int]], Map["VVV", Int](("VVV", 0)), "{\"VVV\":0}")
      verifySerDeser(make[Map[true, Int]], Map[true, Int]((true, 0)), "{\"true\":0}")
      verifySerDeser(make[Map['1', Int]], Map['1', Int](('1', 0)), "{\"1\":0}")
      verifySerDeser(make[Map[2, Int]], Map[2, Int]((2, 0)), "{\"2\":0}")
      verifySerDeser(make[Map[3L, Int]], Map[3L, Int]((3L, 0)), "{\"3\":0}")
      verifySerDeser(make[Map[4.0f, Int]], Map[4.0f, Int]((4.0f, 0)), "{\"4.0\":0}")
      verifySerDeser(make[Map[5.0, Int]], Map[5.0, Int]((5.0, 0)), "{\"5.0\":0}")
    }
    "throw parse exception with expected key in case of illegal input" in {
      verifyDeserError(make[Map["VVV", Int]], "{\"XXX\":0}", "expected key: \"VVV\", offset: 0x00000006")
      verifyDeserError(make[Map[true, Int]], "{\"false\":0}", "expected key: \"true\", offset: 0x00000008")
      verifyDeserError(make[Map['1', Int]], "{\"0\":0}", "expected key: \"1\", offset: 0x00000004")
      verifyDeserError(make[Map[2, Int]], "{\"0\":0}", "expected key: \"2\", offset: 0x00000004")
      verifyDeserError(make[Map[3L, Int]], "{\"0\":0}", "expected key: \"3\", offset: 0x00000004")
      verifyDeserError(make[Map[4.0f, Int]], "{\"0.0\":0}", "expected key: \"4.0\", offset: 0x00000006")
      verifyDeserError(make[Map[5.0, Int]], "{\"0.0\":0}", "expected key: \"5.0\", offset: 0x00000006")
    }
  }
}