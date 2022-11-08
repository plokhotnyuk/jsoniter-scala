package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class MutableBitSetReadingSpec extends BenchmarkSpecBase {
  def benchmark: MutableBitSetReading = new MutableBitSetReading {
    setup()
  }

  "MutableBitSetReading" should {
    "read properly" in {
      benchmark.jsoniterScala() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "true".getBytes(UTF_8)
      intercept[Throwable](b.jsoniterScala())
    }
  }
}