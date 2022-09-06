package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfBigDecimalsReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfBigDecimalsReading = new ArrayOfBigDecimalsReading {
    setup()
  }

  "ArrayOfBigDecimalsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.sourceObj
      benchmark.borer() shouldBe benchmark.sourceObj
      //FIXME circe parses 42667970104045.735577865 as 42667970104045.734
      //benchmark.circe() shouldBe benchmark.sourceObj
      benchmark.circeJawn() shouldBe benchmark.sourceObj
      benchmark.circeJsoniter() shouldBe benchmark.sourceObj
      benchmark.jsoniterScala() shouldBe benchmark.sourceObj
      //FIXME play-json parses 42667970104045.735577865 as 42667970104045.734
      //benchmark.playJson() shouldBe benchmark.sourceObj
      benchmark.playJsonJsoniter() shouldBe benchmark.sourceObj
      benchmark.smithy4sJson() shouldBe benchmark.sourceObj
      benchmark.uPickle() shouldBe benchmark.sourceObj
      benchmark.zioJson() shouldBe benchmark.sourceObj
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
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.zioJson())
    }
  }
}