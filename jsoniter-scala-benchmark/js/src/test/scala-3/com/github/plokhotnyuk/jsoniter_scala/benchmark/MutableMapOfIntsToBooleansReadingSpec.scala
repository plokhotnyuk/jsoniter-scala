package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class MutableMapOfIntsToBooleansReadingSpec extends BenchmarkSpecBase {
  def benchmark: MutableMapOfIntsToBooleansReading = new MutableMapOfIntsToBooleansReading {
    setup()
  }

  "MutableMapOfIntsToBooleansReading" should {
    "read properly" in {
      benchmark.jsoniterScala() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[]".getBytes(UTF_8)
      intercept[Throwable](b.jsoniterScala())
    }
  }
}