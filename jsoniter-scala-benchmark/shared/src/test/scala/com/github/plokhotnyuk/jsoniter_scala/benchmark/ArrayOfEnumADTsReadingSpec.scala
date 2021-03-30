package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfEnumADTsReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfEnumADTsReading = new ArrayOfEnumADTsReading {
    setup()
  }
  
  "ArrayOfEnumADTsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
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
      (0 to 2).foreach { i =>
        val b = benchmark
        b.jsonBytes(i) = 'x'.toByte
        intercept[Throwable](b.avSystemGenCodec())
        intercept[Throwable](b.borer())
        intercept[Throwable](b.circe())
        intercept[Throwable](b.dslJsonScala())
        intercept[Throwable](b.jacksonScala())
        intercept[Throwable](b.jsoniterScala())
        intercept[Throwable](b.playJson())
        intercept[Throwable](b.playJsonJsoniter())
        intercept[Throwable](b.sprayJson())
        intercept[Throwable](b.uPickle())
        intercept[Throwable](b.weePickle())
        intercept[Throwable](b.zioJson())
      }
    }
  }
}