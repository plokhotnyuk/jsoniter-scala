package com.github.plokhotnyuk.jsoniter_scala.macros

class GoogleMapsAPIBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new GoogleMapsAPIBenchmark
  
  "GoogleMapsAPIBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJackson() shouldBe benchmark.obj
      benchmark.readJsoniter() shouldBe benchmark.obj
      benchmark.readPlay() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeJackson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeJsoniter()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterPrealloc()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writePlay()) shouldBe GoogleMapsAPI.compactJsonString
    }
  }
}