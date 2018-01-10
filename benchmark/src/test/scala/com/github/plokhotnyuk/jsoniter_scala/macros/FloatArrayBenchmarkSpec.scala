package com.github.plokhotnyuk.jsoniter_scala.macros

class FloatArrayBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new FloatArrayBenchmark
  
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
      //FIXME: Play-JSON serialize double values instead of float
      //toString(benchmark.writePlay()) shouldBe benchmark.jsonString
    }
  }
}