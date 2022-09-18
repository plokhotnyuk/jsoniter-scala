package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._

class MissingRequiredFieldsReadingSpec extends BenchmarkSpecBase {
  def benchmark: MissingRequiredFieldsReading = new MissingRequiredFieldsReading {
    setup()
  }

  "MissingRequiredFieldsReading" should {
    "return some parsing error" in {
      val b = benchmark
      b.avSystemGenCodec() shouldBe
        "Cannot read com.github.plokhotnyuk.jsoniter_scala.benchmark.MissingRequiredFields, field s is missing in decoded data"
      b.borer() shouldBe
        "Cannot decode `MissingRequiredFields` instance due to missing map keys \"s\" and \"i\" (input position 1)"
      b.circe() shouldBe "Missing required field: DownField(s)"
      b.circeJawn() shouldBe "Missing required field: DownField(s)"
      b.circeJsoniter() shouldBe "Missing required field: DownField(s)"
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
      b.playJson() shouldBe "JsResultException(errors:List((/i,List(JsonValidationError(List(error.path.missing),List()))), (/s,List(JsonValidationError(List(error.path.missing),List())))))"
      b.playJsonJsoniter() shouldBe "JsResultException(errors:List((/i,List(JsonValidationError(List(error.path.missing),List()))), (/s,List(JsonValidationError(List(error.path.missing),List())))))"
      b.smithy4sJson() shouldBe "Missing required field (path: .s)"
      b.uPickle() shouldBe "missing keys in dictionary: s, i at index 1"
      b.zioJson() shouldBe ".s(missing)"
    }
    "return toString value for valid input" in {
      val b = benchmark
      b.jsonBytes = """{"s":"VVV","i":1}""".getBytes(UTF_8)
      b.avSystemGenCodec() shouldBe "MissingRequiredFields(VVV,1)"
      b.borer() shouldBe "MissingRequiredFields(VVV,1)"
      b.circe() shouldBe "MissingRequiredFields(VVV,1)"
      b.circeJawn() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScala() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScalaWithoutDump() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScalaWithStacktrace() shouldBe "MissingRequiredFields(VVV,1)"
      b.playJson() shouldBe "MissingRequiredFields(VVV,1)"
      b.playJsonJsoniter() shouldBe "MissingRequiredFields(VVV,1)"
      b.smithy4sJson() shouldBe "MissingRequiredFields(VVV,1)"
      b.uPickle() shouldBe "MissingRequiredFields(VVV,1)"
      b.zioJson() shouldBe "MissingRequiredFields(VVV,1)"
    }
  }
}