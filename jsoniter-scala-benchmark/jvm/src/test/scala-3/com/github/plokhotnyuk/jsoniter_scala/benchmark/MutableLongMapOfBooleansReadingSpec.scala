package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class MutableLongMapOfBooleansReadingSpec extends BenchmarkSpecBase {
  def benchmark: MutableLongMapOfBooleansReading = new MutableLongMapOfBooleansReading {
    setup()
  }

  "MutableLongMapOfBooleansReading" should {
    "read properly" in {
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      //FIXME: json4s.jackson throws org.json4s.MappingException: unknown error
      //benchmark.json4sJackson() shouldBe benchmark.obj
      //FIXME: json4s.native throws org.json4s.MappingException: unknown error
      //benchmark.json4sNative() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[]".getBytes(UTF_8)
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jacksonScala())
      //FIXME: json4s.jackson throws org.json4s.MappingException: unknown error
      //intercept[Throwable](b.json4sJackson())
      //FIXME: json4s.native throws org.json4s.MappingException: unknown error
      //intercept[Throwable](b.json4sNative())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
    }
  }
}