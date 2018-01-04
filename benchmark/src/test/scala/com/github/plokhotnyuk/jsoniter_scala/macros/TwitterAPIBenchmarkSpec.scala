package com.github.plokhotnyuk.jsoniter_scala.macros

class TwitterAPIBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new TwitterAPIBenchmark
  
  "TwitterAPIBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJackson() shouldBe benchmark.obj
      benchmark.readJsoniter() shouldBe benchmark.obj
      benchmark.readPlay() shouldBe benchmark.obj
    }
    "serialize properly" in {
      // FIXME: circe serializes empty collections
      //toString(benchmark.writeCirce()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeJackson()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeJsoniter()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeJsoniterPrealloc()) shouldBe TwitterAPI.compactJsonString
      // FIXME: Play-JSON serializes empty collections
      //toString(benchmark.writePlay()) shouldBe TwitterAPI.compactJsonString
    }
  }
}