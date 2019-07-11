package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new GoogleMapsAPIWriting
  
  "GoogleMapsAPIWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.borerJson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.circe()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.dslJsonScala()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.jacksonScala()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.jsoniterScala()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.playJson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.scalikeJackson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.sprayJson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.uPickle()) shouldBe GoogleMapsAPI.compactJsonString
    }
  }
}