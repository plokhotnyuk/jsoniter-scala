package com.github.plokhotnyuk.jsoniter_scala.macros

class IntArrayBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new IntArrayBenchmark
  
  "IntArrayBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readJackson().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniter().deep shouldBe benchmark.obj.deep
      benchmark.readPlay().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJackson()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniter()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlay()) shouldBe benchmark.jsonString
    }
  }
}