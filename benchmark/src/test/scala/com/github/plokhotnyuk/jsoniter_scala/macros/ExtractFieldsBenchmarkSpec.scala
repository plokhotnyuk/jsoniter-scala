package com.github.plokhotnyuk.jsoniter_scala.macros

class ExtractFieldsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ExtractFieldsBenchmark
  
  "ExtractFieldsBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJackson() shouldBe benchmark.obj
      benchmark.readJsoniter() shouldBe benchmark.obj
      benchmark.readPlay() shouldBe benchmark.obj
    }
  }
}