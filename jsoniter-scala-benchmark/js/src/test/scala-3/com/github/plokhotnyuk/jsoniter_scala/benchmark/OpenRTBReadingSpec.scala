package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class OpenRTBReadingSpec extends BenchmarkSpecBase {
  def benchmark: OpenRTBReading = new OpenRTBReading {
    setup()
  }

  "OpenRTBReading" should {
    "read properly" in {
      benchmark.borer() shouldBe benchmark.obj
      //FIXME: Circe require a custom codec
      //benchmark.circe() shouldBe benchmark.obj
      //benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.smithy4sJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      //FIXME: zio-json doesn't support default values
      //benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[]".getBytes(UTF_8)
      intercept[Throwable](b.borer())
      //FIXME: Circe require a custom codec
      //intercept[Throwable](b.circe())
      //intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.uPickle())
      //FIXME: zio-json doesn't support default values
      //intercept[Throwable](b.zioJson())
    }
  }
}