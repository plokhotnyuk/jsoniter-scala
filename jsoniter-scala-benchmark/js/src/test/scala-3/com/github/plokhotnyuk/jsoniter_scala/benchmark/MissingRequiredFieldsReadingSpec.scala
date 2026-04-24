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

package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class MissingRequiredFieldsReadingSpec extends BenchmarkSpecBase {
  def benchmark: MissingRequiredFieldsReading = new MissingRequiredFieldsReading {
    setup()
  }

  "MissingRequiredFieldsReading" should {
    "return some parsing error" in {
      val b = benchmark
      b.borer() shouldBe
        "Cannot decode `MissingRequiredFields` instance due to missing map keys \"s\" and \"i\" (input position 1)"
      b.circe() shouldBe "DecodingFailure at .s: Missing required field"
      b.circeJsoniter() shouldBe "DecodingFailure at .s: Missing required field"
      b.jsoniterScala() shouldBe
        """missing required field "s", offset: 0x00000001, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 7b 7d                                           | {}               |
          |+----------+-------------------------------------------------+------------------+""".stripMargin
      b.jsoniterScalaWithoutDump() shouldBe """missing required field "s", offset: 0x00000001"""
      b.jsoniterScalaWithStacktrace() shouldBe
        """missing required field "s", offset: 0x00000001, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 7b 7d                                           | {}               |
          |+----------+-------------------------------------------------+------------------+""".stripMargin
      b.playJson() shouldBe "JsResultException(errors:List((/s,List(JsonValidationError(List(error.path.missing),ArraySeq())))))"
      b.playJsonJsoniter() shouldBe "JsResultException(errors:List((/s,List(JsonValidationError(List(error.path.missing),ArraySeq())))))"
      b.smithy4sJson() shouldBe "Missing required field (path: .s)"
      b.uPickle() shouldBe "missing keys in dictionary: s, i at index 1"
      b.zioBlocks() shouldBe "missing required field \"s\" at: ."
      b.zioJson() shouldBe ".s(missing)"
      b.zioSchemaJson() shouldBe ".s(missing)"
    }
    "return toString value for valid input" in {
      val b = benchmark
      b.jsonBytes = """{"s":"VVV","i":1}""".getBytes(UTF_8)
      b.borer() shouldBe "MissingRequiredFields(VVV,1)"
      b.circe() shouldBe "MissingRequiredFields(VVV,1)"
      b.circeJsoniter() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScala() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScalaWithoutDump() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScalaWithStacktrace() shouldBe "MissingRequiredFields(VVV,1)"
      b.playJson() shouldBe "MissingRequiredFields(VVV,1)"
      b.playJsonJsoniter() shouldBe "MissingRequiredFields(VVV,1)"
      b.smithy4sJson() shouldBe "MissingRequiredFields(VVV,1)"
      b.uPickle() shouldBe "MissingRequiredFields(VVV,1)"
      b.zioBlocks() shouldBe "MissingRequiredFields(VVV,1)"
      b.zioJson() shouldBe "MissingRequiredFields(VVV,1)"
      b.zioSchemaJson() shouldBe "MissingRequiredFields(VVV,1)"
    }
  }
}