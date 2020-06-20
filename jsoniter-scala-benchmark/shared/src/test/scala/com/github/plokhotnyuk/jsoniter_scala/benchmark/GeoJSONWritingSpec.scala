package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GeoJSONWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new GeoJSONWriting
  
  "GeoJSONWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString1
      toString(benchmark.borer()) shouldBe benchmark.jsonString1
      toString(benchmark.circe()) shouldBe benchmark.jsonString2
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString1
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString1
      toString(benchmark.playJson()) shouldBe benchmark.jsonString1
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString4
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString1
      toString(benchmark.weePickle()) shouldBe benchmark.jsonString1
    }
  }
}