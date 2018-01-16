package com.github.plokhotnyuk.jsoniter_scala.macros

class IntAndLongMapsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new IntAndLongMapsBenchmark
  
  "IntAndLongMapsBenchmark" should {
    "deserialize properly" in {
      //FIXME: Circe doesn't support parsing of int & long maps
      //benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: Jackson-module-scala doesn't support parsing of int & long maps
      //benchmark.readJackson() shouldBe benchmark.obj
      benchmark.readJsoniter() shouldBe benchmark.obj
      benchmark.readPlay() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: Circe doesn't support writing of int & long maps
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeJackson()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniter()) shouldBe benchmark.jsonString
      toString(benchmark.writePlay()) shouldBe benchmark.jsonString
    }
  }
}