package com.github.plokhotnyuk.jsoniter_scala.macros

class IntAndLongMapsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new IntAndLongMapsBenchmark
  
  "IntAndLongMapsBenchmark" should {
    "deserialize properly" in {
      //FIXME: Circe doesn't support parsing of int & long maps
      //benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: Jackson-module-scala doesn't support parsing of int & long maps
      //benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: Circe doesn't support writing of int & long maps
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
      //toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}