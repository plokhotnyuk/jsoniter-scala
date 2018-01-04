package com.github.plokhotnyuk.jsoniter_scala.macros

class ArraysBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArraysBenchmark
  
  "ArraysBenchmark" should {
    "deserialize properly" in {
      assertArrays(benchmark.readCirce(), benchmark.obj)
      assertArrays(benchmark.readJackson(), benchmark.obj)
      assertArrays(benchmark.readJsoniter(), benchmark.obj)
      assertArrays(benchmark.readPlay(), benchmark.obj)
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJackson()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniter()) shouldBe benchmark.jsonString
      toString(benchmark.writePlay()) shouldBe benchmark.jsonString
    }
  }
}