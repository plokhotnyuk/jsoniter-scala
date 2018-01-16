package com.github.plokhotnyuk.jsoniter_scala.macros

class BitSetsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new BitSetsBenchmark
  
  "BitSetsBenchmark" should {
    "deserialize properly" in {
      //FIXME: Circe doesn't support parsing of bitsets
      //benchmark.readCirce() shouldBe benchmark.bitSetsObj
      benchmark.readJackson() shouldBe benchmark.obj
      benchmark.readJsoniter() shouldBe benchmark.obj
      benchmark.readPlay() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: Circe doesn't support writing of bitsets
      //toString(benchmark.writeCirce()) shouldBe benchmark.bitSetsJsonString
      toString(benchmark.writeJackson()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniter()) shouldBe benchmark.jsonString
      toString(benchmark.writePlay()) shouldBe benchmark.jsonString
    }
  }
}