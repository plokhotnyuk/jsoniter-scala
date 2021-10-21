package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ADTReadingSpec extends BenchmarkSpecBase {
  def benchmark = new ADTReading

  "ADTReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.playJsonJsoniter() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
      benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(8) = '1'.toByte
      b.jsonBytes(9) = '1'.toByte
      b.jsonBytes(10) = '1'.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.weePickle())
      intercept[Throwable](b.zioJson())
    }
    "fail on invalid discriminator value" in {
      val b = benchmark
      b.jsonBytes(9) = 'x'.toByte
      intercept[Throwable](b.sprayJson())
    }
  }
}