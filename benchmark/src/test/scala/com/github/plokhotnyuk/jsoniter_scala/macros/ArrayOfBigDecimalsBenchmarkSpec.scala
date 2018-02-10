package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfBigDecimalsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArrayOfBigDecimalsBenchmark
  
  "ArrayOfBigDecimalsBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce().deep shouldBe benchmark.sourceObj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.sourceObj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.sourceObj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.sourceObj.deep
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}