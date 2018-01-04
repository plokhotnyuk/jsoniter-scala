package com.github.plokhotnyuk.jsoniter_scala.macros

class AdtBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new AdtBenchmark
  
  "AdtBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJackson() shouldBe benchmark.obj
      benchmark.readJsoniter() shouldBe benchmark.obj
      benchmark.readPlay() shouldBe benchmark.obj
    }
    "serialize properly" in {
      // FIXME: circe appends discriminator as a last field
      //toString(benchmark.writeAdtCirce()) shouldBe benchmark.adtJsonString
      toString(benchmark.writeJackson()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniter()) shouldBe benchmark.jsonString
      toString(benchmark.writePlay()) shouldBe benchmark.jsonString
    }
  }
}