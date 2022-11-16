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
      benchmark.jacksonScala() shouldBe benchmark.obj
      //FIXME: json4s.jackson throws org.json4s.MappingException: Can't find ScalaSig for class com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON$FeatureCollection
      //benchmark.json4sJackson() shouldBe benchmark.obj
      //FIXME: json4s.native throws org.json4s.MappingException: Can't find ScalaSig for class com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON$FeatureCollection
      //benchmark.json4sNative() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.smithy4sJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
      //FIXME: zio-json throws java.lang.RuntimeException: (FeatureCollection).features[0](Feature).geometry(invalid disambiguator)
      //benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[]".getBytes(UTF_8)
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jacksonScala())
      //FIXME: json4s.jackson throws org.json4s.MappingException: Can't find ScalaSig for class com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON$FeatureCollection
      //intercept[Throwable](b.json4sJackson())
      //FIXME: json4s.native throws org.json4s.MappingException: Can't find ScalaSig for class com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON$FeatureCollection
      //intercept[Throwable](b.json4sNative())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.weePickle())
      //FIXME: zio-json throws java.lang.RuntimeException: (FeatureCollection).features[0](Feature).geometry(invalid disambiguator)
      //intercept[Throwable](b.zioJson())
    }
  }
}