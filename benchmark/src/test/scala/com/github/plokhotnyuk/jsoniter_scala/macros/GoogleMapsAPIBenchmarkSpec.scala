package com.github.plokhotnyuk.jsoniter_scala.macros

class GoogleMapsAPIBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new GoogleMapsAPIBenchmark
  
  "GoogleMapsAPIBenchmark" should {
    "deserialize properly" in {
      benchmark.readGoogleMapsAPICirce() shouldBe benchmark.obj
      benchmark.readGoogleMapsAPIJackson() shouldBe benchmark.obj
      benchmark.readGoogleMapsAPIJsoniter() shouldBe benchmark.obj
      benchmark.readGoogleMapsAPIPlay() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeGoogleMapsAPICirce()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeGoogleMapsAPIJackson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeGoogleMapsAPIJsoniter()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeGoogleMapsAPIJsoniterPrealloc()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeGoogleMapsAPIPlay()) shouldBe GoogleMapsAPI.compactJsonString
    }
  }
}