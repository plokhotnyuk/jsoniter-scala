package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GeoJSONReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new GeoJSONReading
  
  "GeoJSONReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      //FIXME: upickle.core.AbortException: invalid tag for tagged object: com.github.plokhotnyuk.jsoniter_scala.benchmark.FeatureCollection at index 8
      //benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}