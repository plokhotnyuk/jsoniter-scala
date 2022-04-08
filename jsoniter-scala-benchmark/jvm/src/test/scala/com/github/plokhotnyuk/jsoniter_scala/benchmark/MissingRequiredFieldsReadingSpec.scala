package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._

class MissingRequiredFieldsReadingSpec extends BenchmarkSpecBase {
  def benchmark = new MissingRequiredFieldsReading

  "MissingRequiredFieldsReading" should {
    "return some parsing error" in {
      val b = benchmark
      b.avSystemGenCodec() shouldBe
        "Cannot read com.github.plokhotnyuk.jsoniter_scala.benchmark.MissingRequiredFields, field s is missing in decoded data"
      b.borer() shouldBe
        "Cannot decode `MissingRequiredFields` instance due to missing map keys \"s\" and \"i\" (input position 1)"
      b.circe() shouldBe "Attempt to decode value on failed cursor: DownField(s)"
      b.circeJawn() shouldBe "Attempt to decode value on failed cursor: DownField(s)"
      b.circeJsoniter() shouldBe "Attempt to decode value on failed cursor: DownField(s)"
      b.dslJsonScala() shouldBe
        "Mandatory properties (s, i) not found at position: 1, following: `{`, before: `}`"
      b.jacksonScala() shouldBe
        """Missing required creator property 's' (index 0)
          | at [Source: (byte[])"{}"; line: 1, column: 2] (through reference chain: com.github.plokhotnyuk.jsoniter_scala.benchmark.MissingRequiredFields["s"])""".stripMargin
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
      b.playJson() should include("JsResultException")
      b.playJsonJsoniter() should include("JsResultException")
      b.sprayJson() shouldBe "Object is missing required member 's'"
      b.uPickle() shouldBe "missing keys in dictionary: s, i at index 1"
      b.weePickle() shouldBe "Parser or Visitor failure jsonPointer= index=2 line=1 col=3 token=END_OBJECT"
      b.zioJson() shouldBe ".s(missing)"
    }
    "return toString value for valid input" in {
      val b = benchmark
      b.jsonBytes = """{"s":"VVV","i":1}""".getBytes(UTF_8)
      b.avSystemGenCodec() shouldBe "MissingRequiredFields(VVV,1)"
      b.borer() shouldBe "MissingRequiredFields(VVV,1)"
      b.circe() shouldBe "MissingRequiredFields(VVV,1)"
      b.circeJawn() shouldBe "MissingRequiredFields(VVV,1)"
      b.dslJsonScala() shouldBe "MissingRequiredFields(VVV,1)"
      b.jacksonScala() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScala() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScalaWithoutDump() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScalaWithStacktrace() shouldBe "MissingRequiredFields(VVV,1)"
      b.playJson() shouldBe "MissingRequiredFields(VVV,1)"
      b.playJsonJsoniter() shouldBe "MissingRequiredFields(VVV,1)"
      b.sprayJson() shouldBe "MissingRequiredFields(VVV,1)"
      b.uPickle() shouldBe "MissingRequiredFields(VVV,1)"
      b.weePickle() shouldBe "MissingRequiredFields(VVV,1)"
      b.zioJson() shouldBe "MissingRequiredFields(VVV,1)"
    }
  }
}