package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BigDecimalReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new BigDecimalReading {
    setup()
  }
  
  "BigDecimalReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.sourceObj
      benchmark.circe() shouldBe benchmark.sourceObj
      benchmark.dslJsonScala() shouldBe benchmark.sourceObj
      benchmark.jacksonScala() shouldBe benchmark.sourceObj
      benchmark.jsoniterScala() shouldBe benchmark.sourceObj
      //FIXME: Play-JSON: don't know how to tune precision for parsing of BigDecimal values
      //benchmark.playJson() shouldBe benchmark.sourceObj
      benchmark.scalikeJackson() shouldBe benchmark.sourceObj
      benchmark.sprayJson() shouldBe benchmark.sourceObj
      benchmark.uPickle() shouldBe benchmark.sourceObj
    }
  }
}