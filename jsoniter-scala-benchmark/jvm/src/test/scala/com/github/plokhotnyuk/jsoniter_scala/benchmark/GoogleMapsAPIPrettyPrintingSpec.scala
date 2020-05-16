package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIPrettyPrintingSpec extends BenchmarkSpecBase {
  private val benchmark = new GoogleMapsAPIPrettyPrinting
  
  "GoogleMapsAPIPrettyPrinting" should {
    "pretty print properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString2
      toString(benchmark.circe()) shouldBe benchmark.jsonString1
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString1
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString2
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString2
      toString(benchmark.playJson()) shouldBe benchmark.jsonString1
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString2
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString2
      toString(benchmark.weePickle()) shouldBe benchmark.jsonString2
    }
  }
}