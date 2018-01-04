package com.github.plokhotnyuk.jsoniter_scala.macros

class MutableMapsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new MutableMapsBenchmark
  
  "MutableMapsBenchmark" should {
    "deserialize properly" in {
      //FIXME: Circe doesn't support parsing of mutable maps
      //benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: Jackson-module-scala parse keys as String
      //benchmark.readJackson() shouldBe benchmark.obj
      benchmark.readJsoniter() shouldBe benchmark.obj
      benchmark.readPlay() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      // FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeJackson()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniter()) shouldBe benchmark.jsonString
      toString(benchmark.writePlay()) shouldBe benchmark.jsonString
    }
  }
}