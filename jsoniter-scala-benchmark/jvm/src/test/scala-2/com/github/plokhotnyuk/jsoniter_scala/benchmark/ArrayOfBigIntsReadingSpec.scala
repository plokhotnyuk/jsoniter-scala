package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class ArrayOfBigIntsReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfBigIntsReading = new ArrayOfBigIntsReading {
    setup()
  }

  "ArrayOfBigIntsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.json4sJackson() shouldBe benchmark.obj
      benchmark.json4sNative() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      // FIXME: Play-JSON looses significant digits in BigInt values
      // benchmark.playJson() shouldBe benchmark.obj
      // FIXME: Play-JSON looses significant digits in BigInt values
      // benchmark.playJsonJsoniter() shouldBe benchmark.obj
      benchmark.smithy4sJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
      benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[true]".getBytes(UTF_8)
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.dslJsonScala())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.json4sJackson())
      intercept[Throwable](b.json4sNative())
      intercept[Throwable](b.jsoniterScala())
      // FIXME: Play-JSON looses significant digits in BigInt values
      // intercept[Throwable](b.playJson())
      // FIXME: Play-JSON looses significant digits in BigInt values
      // intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.weePickle())
      intercept[Throwable](b.zioJson())
    }
  }
}