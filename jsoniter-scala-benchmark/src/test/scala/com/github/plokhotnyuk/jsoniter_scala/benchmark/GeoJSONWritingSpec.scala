package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GeoJSONWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new GeoJSONWriting
  
  "GeoJSONWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe GeoJSON.jsonString1
      toString(benchmark.borerJson()) shouldBe GeoJSON.jsonString1
      toString(benchmark.circe()) shouldBe GeoJSON.jsonString2
      toString(benchmark.jacksonScala()) shouldBe GeoJSON.jsonString1
      toString(benchmark.jsoniterScala()) shouldBe GeoJSON.jsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe GeoJSON.jsonString1
      toString(benchmark.playJson()) shouldBe GeoJSON.jsonString1
      toString(benchmark.sprayJson()) shouldBe GeoJSON.jsonString4
      toString(benchmark.uPickle()) shouldBe GeoJSON.jsonString1
      toString(benchmark.weePickle()) shouldBe GeoJSON.jsonString1
    }
  }
}