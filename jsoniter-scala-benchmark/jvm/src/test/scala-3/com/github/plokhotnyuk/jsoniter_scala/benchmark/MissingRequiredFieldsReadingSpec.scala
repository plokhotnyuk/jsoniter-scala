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
      b.circe() shouldBe "Missing required field: DownField(s)"
      b.circeJsoniter() shouldBe "Missing required field: DownField(s)"
      b.jacksonScala() shouldBe
        """Null value for creator property 's' (index 0); `DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES` enabled
          | at [Source: (byte[])"{}"; line: 1, column: 2] (through reference chain: com.github.plokhotnyuk.jsoniter_scala.benchmark.MissingRequiredFields["s"])""".stripMargin
      b.json4sJackson() shouldBe
        """No usable value for s
          |Did not find value which can be converted into java.lang.String""".stripMargin
      b.json4sNative() shouldBe
        """No usable value for s
          |Did not find value which can be converted into java.lang.String""".stripMargin
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
      b.smithy4sJson() shouldBe "Missing required field (path: .s)"
      b.sprayJson() shouldBe "Object is missing required member 's'"
      b.uPickle() shouldBe "missing keys in dictionary: s, i at index 1"
      b.weePickle() shouldBe "Parser or Visitor failure jsonPointer= index=2 line=1 col=3 token=END_OBJECT"
      b.zioJson() shouldBe ".s(missing)"
    }
    "return toString value for valid input" in {
      val b = benchmark
      b.jsonBytes = """{"s":"VVV","i":1}""".getBytes(UTF_8)
      b.borer() shouldBe "MissingRequiredFields(VVV,1)"
      b.circe() shouldBe "MissingRequiredFields(VVV,1)"
      b.circeJsoniter() shouldBe "MissingRequiredFields(VVV,1)"
      b.jacksonScala() shouldBe "MissingRequiredFields(VVV,1)"
      b.json4sJackson() shouldBe "MissingRequiredFields(VVV,1)"
      b.json4sNative() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScala() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScalaWithoutDump() shouldBe "MissingRequiredFields(VVV,1)"
      b.jsoniterScalaWithStacktrace() shouldBe "MissingRequiredFields(VVV,1)"
      b.playJson() shouldBe "MissingRequiredFields(VVV,1)"
      b.smithy4sJson() shouldBe "MissingRequiredFields(VVV,1)"
      b.sprayJson() shouldBe "MissingRequiredFields(VVV,1)"
      b.uPickle() shouldBe "MissingRequiredFields(VVV,1)"
      b.weePickle() shouldBe "MissingRequiredFields(VVV,1)"
      b.zioJson() shouldBe "MissingRequiredFields(VVV,1)"
    }
  }
}