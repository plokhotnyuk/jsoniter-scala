package com.github.plokhotnyuk.jsoniter_scala.macros

class TwitterAPIBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new TwitterAPIBenchmark
  
  "TwitterAPIBenchmark" should {
    "deserialize properly" in {
      benchmark.readTwitterAPICirce() shouldBe benchmark.obj
      benchmark.readTwitterAPIJackson() shouldBe benchmark.obj
      benchmark.readTwitterAPIJsoniter() shouldBe benchmark.obj
      benchmark.readTwitterAPIPlay() shouldBe benchmark.obj
    }
    "serialize properly" in {
      // FIXME: circe serializes empty collections
      //toString(benchmark.writeTwitterAPICirce()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeTwitterAPIJackson()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeTwitterAPIJsoniter()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeTwitterAPIJsoniterPrealloc()) shouldBe TwitterAPI.compactJsonString
      // FIXME: Play-JSON serializes empty collections
      //toString(benchmark.writeTwitterAPIPlay()) shouldBe TwitterAPI.compactJsonString
    }
  }
}