package com.github.plokhotnyuk.jsoniter_scala.benchmark

class Base16ReadingSpec extends BenchmarkSpecBase {
  private def benchmark = new Base16Reading {
    setup()
  }
  
  "Base16Reading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(0) = 'x'.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.jsoniterScala())
    }
  }
}