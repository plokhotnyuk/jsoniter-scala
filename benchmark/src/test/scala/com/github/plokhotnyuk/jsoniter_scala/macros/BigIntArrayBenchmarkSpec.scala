package com.github.plokhotnyuk.jsoniter_scala.macros

class BigIntArrayBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new BigIntArrayBenchmark
  
  "BigIntArrayBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readJackson().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniter().deep shouldBe benchmark.obj.deep
      //FIXME: add format for BigInt arrays
      //benchmark.readPlay().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      //FIXME: Circe uses an engineering decimal notation to serialize BigInt
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJackson()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniter()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterPrealloc()) shouldBe benchmark.jsonString
      //FIXME: add format for BigInt arrays
      //toString(benchmark.writePlay()) shouldBe benchmark.jsonString
    }
  }
}