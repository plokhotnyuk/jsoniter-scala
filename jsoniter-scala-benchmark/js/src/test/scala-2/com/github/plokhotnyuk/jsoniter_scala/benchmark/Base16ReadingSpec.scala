package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class Base16ReadingSpec extends BenchmarkSpecBase {
  def benchmark: Base16Reading = new Base16Reading {
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
      b.jsonBytes = "{}".getBytes(UTF_8)
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.jsoniterScala())
    }
  }
}