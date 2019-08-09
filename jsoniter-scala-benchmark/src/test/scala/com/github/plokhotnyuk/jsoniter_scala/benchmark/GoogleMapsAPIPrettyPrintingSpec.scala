package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIPrettyPrintingSpec extends BenchmarkSpecBase {
  private val benchmark = new GoogleMapsAPIPrettyPrinting
  
  "GoogleMapsAPIPrettyPrinting" should {
    "pretty print properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.circe()) shouldBe GoogleMapsAPI.jsonString1
      //FIXME: DSL-JSON doesn't support pretty printing
      //toString(benchmark.dslJsonScala()) shouldBe GoogleMapsAPI.jsonString1
      //FIXME: Jackson throws java.lang.IllegalStateException: Failed `createInstance()`: com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers$$anon$3 does not override method; it has to
      //toString(benchmark.jacksonScala()) shouldBe GoogleMapsAPI.jsonString1
      toString(benchmark.jsoniterScala()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe GoogleMapsAPI.jsonString2
      //FIXME: Play-JSON throws java.lang.IllegalStateException: Failed `createInstance()`: com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats$$anon$1$$anon$2 does not override method; it has to
      //toString(benchmark.playJson()) shouldBe GoogleMapsAPI.jsonString1
      toString(benchmark.sprayJson()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.uPickle()) shouldBe GoogleMapsAPI.jsonString2
    }
  }
}