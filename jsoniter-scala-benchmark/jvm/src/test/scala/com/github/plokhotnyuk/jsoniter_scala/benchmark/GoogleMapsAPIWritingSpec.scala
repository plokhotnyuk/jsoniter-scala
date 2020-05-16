package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new GoogleMapsAPIWriting
  
  "GoogleMapsAPIWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.compactJsonString
      toString(benchmark.borer()) shouldBe benchmark.compactJsonString
      toString(benchmark.circe()) shouldBe benchmark.compactJsonString
      toString(benchmark.dslJsonScala()) shouldBe benchmark.compactJsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.compactJsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.compactJsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.compactJsonString
      toString(benchmark.playJson()) shouldBe benchmark.compactJsonString
      toString(benchmark.sprayJson()) shouldBe benchmark.compactJsonString
      toString(benchmark.uPickle()) shouldBe benchmark.compactJsonString
      toString(benchmark.weePickle()) shouldBe benchmark.compactJsonString
    }
  }
}