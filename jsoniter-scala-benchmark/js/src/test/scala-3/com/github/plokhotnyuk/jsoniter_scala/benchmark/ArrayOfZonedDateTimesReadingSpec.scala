package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class ArrayOfZonedDateTimesReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfZonedDateTimesReading = new ArrayOfZonedDateTimesReading {
    setup()
  }

  "ArrayOfZonedDateTimesReading" should {
    "read properly" in {
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[true]".getBytes(UTF_8)
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.uPickle())
    }
  }
}