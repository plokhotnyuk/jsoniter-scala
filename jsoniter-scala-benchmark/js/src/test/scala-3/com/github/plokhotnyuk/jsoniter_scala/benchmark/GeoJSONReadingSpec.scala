package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class GeoJSONReadingSpec extends BenchmarkSpecBase {
  def benchmark: GeoJSONReading = new GeoJSONReading {
    setup()
  }

  "GeoJSONReading" should {
    "read properly" in {
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.smithy4sJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      //FIXME: zio-json throws java.lang.RuntimeException: (FeatureCollection).features[0](Feature).geometry(invalid disambiguator)
      //benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[]".getBytes(UTF_8)
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.uPickle())
      //FIXME: zio-json throws java.lang.RuntimeException: (FeatureCollection).features[0](Feature).geometry(invalid disambiguator)
      //intercept[Throwable](b.zioJson())
    }
  }
}