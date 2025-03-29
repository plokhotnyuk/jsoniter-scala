package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class NestedStructsReadingSpec extends BenchmarkSpecBase {
  def benchmark: NestedStructsReading = new NestedStructsReading {
    setup()
  }

  "NestedStructsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      // FIXME: Borer throws io.bullet.borer.Borer$Error$Overflow: This JSON parser does not support more than 64 Array/Object nesting levels
      // benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.playJsonJsoniter() shouldBe benchmark.obj
      benchmark.smithy4sJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.zioJson() shouldBe benchmark.obj
      benchmark.zioSchemaJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "x".getBytes(UTF_8)
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.zioJson())
      intercept[Throwable](b.zioSchemaJson())
    }
  }
}