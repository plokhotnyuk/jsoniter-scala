package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfBigDecimalsReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfBigDecimalsReading = new ArrayOfBigDecimalsReading {
    setup()
  }

  "ArrayOfBigDecimalsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.sourceObj
      benchmark.borer() shouldBe benchmark.sourceObj
      benchmark.circe() shouldBe benchmark.sourceObj
      //FIXME: circe API parse whole numbers as long values
      //benchmark.circeJsoniter() shouldBe benchmark.sourceObj
      benchmark.dslJsonScala() shouldBe benchmark.sourceObj
      benchmark.jacksonScala() shouldBe benchmark.sourceObj
      benchmark.jsoniterScala() shouldBe benchmark.sourceObj
      benchmark.playJson() shouldBe benchmark.sourceObj
      benchmark.playJsonJsoniter() shouldBe benchmark.sourceObj
      benchmark.sprayJson() shouldBe benchmark.sourceObj
      benchmark.uPickle() shouldBe benchmark.sourceObj
      //FIXME: weePickle rounds mantissa to 16 digits
      //benchmark.weePickle() shouldBe benchmark.sourceObj
      benchmark.zioJson() shouldBe benchmark.sourceObj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(0) = 'x'.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      //FIXME: circe API parse whole numbers as long values
      //intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.dslJsonScala())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.zioJson())
    }
  }
}