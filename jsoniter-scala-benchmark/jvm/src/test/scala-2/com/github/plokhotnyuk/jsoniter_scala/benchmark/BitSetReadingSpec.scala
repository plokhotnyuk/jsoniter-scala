package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class BitSetReadingSpec extends BenchmarkSpecBase {
  def benchmark: BitSetReading = new BitSetReading {
    setup()
  }

  "BitSetReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      //FIXME: DSL-JSON throws scala.collection.immutable.HashSet$HashTrieSet cannot be cast to scala.collection.immutable.BitSet
      //benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.json4sJackson() shouldBe benchmark.obj
      benchmark.json4sNative() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.playJsonJsoniter() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "{}".getBytes(UTF_8)
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.json4sJackson())
      intercept[Throwable](b.json4sNative())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
    }
  }
}