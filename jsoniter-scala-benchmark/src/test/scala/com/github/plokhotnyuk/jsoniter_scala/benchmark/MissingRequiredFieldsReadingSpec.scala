package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MissingRequiredFieldsReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new MissingRequiredFieldsReading
  
  "MissingRequiredFieldsReading" should {
    "return some parsing error" in {
      benchmark.avSystemGenCodec() shouldBe
        "Cannot read com.github.plokhotnyuk.jsoniter_scala.benchmark.MissingRequiredFields, field s is missing in decoded data"
      benchmark.circe() shouldBe
        "Attempt to decode value on failed cursor: DownField(s)"
      benchmark.dslJsonScala() shouldBe
        "Mandatory properties (s, i) not found at position: 1, following: `{`, before: `}`"
      benchmark.jacksonScala() shouldBe
        """Missing required creator property 's' (index 0)
          | at [Source: (byte[])"{}"; line: 1, column: 2]""".stripMargin
      benchmark.jsoniterScala() shouldBe
        """missing required field "s", offset: 0x00000001, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 7b 7d                                           | {}               |
          |+----------+-------------------------------------------------+------------------+""".stripMargin
      benchmark.jsoniterScalaWithoutDump() shouldBe
        """missing required field "s", offset: 0x00000001"""
      benchmark.jsoniterScalaWithStacktrace() shouldBe
        """missing required field "s", offset: 0x00000001, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 7b 7d                                           | {}               |
          |+----------+-------------------------------------------------+------------------+""".stripMargin
      benchmark.playJson() should include("JsResultException")
      //FIXME: ScalikeJackson throws an exception with unexpected message: "No MissingRequiredFields ScalaJacksonFormat found for json input"
      //benchmark.scalikeJackson() shouldBe
      //  "???"
      benchmark.sprayJson() shouldBe
        "Object is missing required member 's'"
      benchmark.uPickle() shouldBe
        "missing keys in dictionary: s, i at index 1"
    }
  }
}