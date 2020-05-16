package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfBigDecimalsReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBigDecimalsReading {
    setup()
  }
  
  "ArrayOfBigDecimalsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.sourceObj
      benchmark.borer() shouldBe benchmark.sourceObj
      benchmark.circe() shouldBe benchmark.sourceObj
      benchmark.dslJsonScala() shouldBe benchmark.sourceObj
      benchmark.jacksonScala() shouldBe benchmark.sourceObj
      benchmark.jsoniterScala() shouldBe benchmark.sourceObj
      benchmark.playJson() shouldBe benchmark.sourceObj
      benchmark.sprayJson() shouldBe benchmark.sourceObj
      benchmark.uPickle() shouldBe benchmark.sourceObj
      //FIXME: weePickle rounds mantissa to 16 digits
      //benchmark.weePickle() shouldBe benchmark.sourceObj
    }
  }
}