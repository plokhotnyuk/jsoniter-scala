package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableBitSetReadingSpec extends BenchmarkSpecBase {
  def benchmark: MutableBitSetReading = new MutableBitSetReading {
    setup()
  }

  "MutableBitSetReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJawn() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.playJsonJsoniter() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(0) = 'x'.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
    }
  }
}