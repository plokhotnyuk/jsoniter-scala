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
      benchmark.json4sJackson() shouldBe benchmark.obj
      benchmark.json4sNative() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.playJsonJsoniter() shouldBe benchmark.obj
      benchmark.smithy4sJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
      // FIXME: zio-blocks-schema does not flatten the trait hierarchy
      //benchmark.zioBlocks() shouldBe benchmark.obj
      benchmark.zioJson() shouldBe benchmark.obj
      // FIXME: zio-schema-json throws java.lang.RuntimeException: .type.FeatureCollection.features[0].type.Feature.geometry.type.Polygon(unrecognized subtype)
      // benchmark.zioSchemaJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[]".getBytes(UTF_8)
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.json4sJackson())
      intercept[Throwable](b.json4sNative())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.weePickle())
      // FIXME: zio-blocks-schema does not flatten the trait hierarchy
      //intercept[Throwable](b.zioBlocks())
      intercept[Throwable](b.zioJson())
      // FIXME: zio-schema-json throws java.lang.RuntimeException: .type.FeatureCollection.features[0].type.Feature.geometry.type.Polygon(unrecognized subtype)
      // intercept[Throwable](b.zioSchemaJson())
    }
  }
}