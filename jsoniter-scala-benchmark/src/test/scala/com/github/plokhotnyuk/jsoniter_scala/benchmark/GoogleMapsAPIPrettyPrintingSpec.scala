package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIPrettyPrintingSpec extends BenchmarkSpecBase {
  private val benchmark = new GoogleMapsAPIPrettyPrinting
  
  "GoogleMapsAPIPrettyPrinting" should {
    "pretty print properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.circe()) shouldBe GoogleMapsAPI.jsonString1
      //FIXME: DSL-JSON doesn't support pretty printing
      //toString(benchmark.dslJsonScala()) shouldBe GoogleMapsAPI.jsonString1
      toString(benchmark.jacksonScala()) shouldBe GoogleMapsAPI.jsonString1
      toString(benchmark.jsoniterScala()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.playJson()) shouldBe GoogleMapsAPI.jsonString1
      toString(benchmark.sprayJson()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.uPickle()) shouldBe GoogleMapsAPI.jsonString2
    }
  }
}