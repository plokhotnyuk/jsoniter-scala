package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class ArrayOfZoneOffsetsReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfZoneOffsetsReading = new ArrayOfZoneOffsetsReading {
    setup()
  }

  "ArrayOfZoneOffsetsReading" should {
    "read properly" in {
      benchmark.borer() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[true]".getBytes(UTF_8)
      intercept[Throwable](b.borer())
      intercept[Throwable](b.jsoniterScala())
    }
  }
}