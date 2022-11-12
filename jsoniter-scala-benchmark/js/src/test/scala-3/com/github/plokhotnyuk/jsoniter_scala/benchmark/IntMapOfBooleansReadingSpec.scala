package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class IntMapOfBooleansReadingSpec extends BenchmarkSpecBase {
  def benchmark: IntMapOfBooleansReading = new IntMapOfBooleansReading {
    setup()
  }

  "IntMapOfBooleansReading" should {
    "read properly" in {
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      //FIXME: uPickle doesn't support IntMap
      //benchmark.uPickle() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[]".getBytes(UTF_8)
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jsoniterScala())
    }
  }
}