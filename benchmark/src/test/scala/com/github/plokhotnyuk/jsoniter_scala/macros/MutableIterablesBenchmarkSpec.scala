package com.github.plokhotnyuk.jsoniter_scala.macros

class MutableIterablesBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new MutableIterablesBenchmark
  
  "MutableIterablesBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: Jackson-module-scala doesn't support parsing of tree sets
      //benchmark.readJackson() shouldBe benchmark.obj
      benchmark.readJsoniter() shouldBe benchmark.obj
      benchmark.readPlay() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJackson()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniter()) shouldBe benchmark.jsonString
      toString(benchmark.writePlay()) shouldBe benchmark.jsonString
    }
  }
}