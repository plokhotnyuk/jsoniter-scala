package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class MapOfIntsToBooleansReadingSpec extends BenchmarkSpecBase {
  def benchmark: MapOfIntsToBooleansReading = new MapOfIntsToBooleansReading {
    setup()
  }

  "MapOfIntsToBooleansReading" should {
    "read properly" in {
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.json4sJackson() shouldBe benchmark.obj
      benchmark.json4sNative() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.smithy4sJson() shouldBe benchmark.obj
      //FIXME: uPickle parses maps from JSON arrays only
      //benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[]".getBytes(UTF_8)
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.json4sJackson())
      intercept[Throwable](b.json4sNative())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.weePickle())
    }
  }
}