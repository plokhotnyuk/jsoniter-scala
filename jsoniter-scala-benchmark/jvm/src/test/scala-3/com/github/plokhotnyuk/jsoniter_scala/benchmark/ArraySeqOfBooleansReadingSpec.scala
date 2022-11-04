package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class ArraySeqOfBooleansReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArraySeqOfBooleansReading = new ArraySeqOfBooleansReading {
    setup()
  }

  "ArraySeqOfBooleansReading" should {
    "read properly" in {
      benchmark.borer() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      //FIXME json4s.jackson throws org.json4s.MappingException: unknown error
      //benchmark.json4sJackson() shouldBe benchmark.obj
      //FIXME json4s.native throws org.json4s.MappingException: unknown error
      //benchmark.json4sNative() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.smithy4sJson() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[{}]".getBytes(UTF_8)
      intercept[Throwable](b.borer())
      intercept[Throwable](b.jacksonScala())
      //FIXME json4s.jackson throws org.json4s.MappingException: unknown error
      //intercept[Throwable](b.json4sJackson())
      //FIXME json4s.native throws org.json4s.MappingException: unknown error
      //intercept[Throwable](b.json4sNative())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.weePickle())
    }
  }
}