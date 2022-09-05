package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BigDecimalReadingSpec extends BenchmarkSpecBase {
  def benchmark: BigDecimalReading = new BigDecimalReading {
    setup()
  }

  "BigDecimalReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.sourceObj
      benchmark.borer() shouldBe benchmark.sourceObj
      benchmark.circe() shouldBe benchmark.sourceObj
      benchmark.circeJawn() shouldBe benchmark.sourceObj
      benchmark.circeJsoniter() shouldBe benchmark.sourceObj
      benchmark.jsoniterScala() shouldBe benchmark.sourceObj
      //FIXME: Play-JSON: don't know how to tune precision for parsing of BigDecimal values
      //benchmark.playJson() shouldBe benchmark.sourceObj
      //FIXME: smithy4sJson: don't know how to tune precision for parsing of BigDecimal values
      //benchmark.smithy4sJson() shouldBe benchmark.sourceObj
      benchmark.uPickle() shouldBe benchmark.sourceObj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(0) = 'x'.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJawn())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.uPickle())
    }
  }
}