package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfOffsetTimesBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArrayOfOffsetTimesBenchmark
  
  "ArrayOfOffsetTimesBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      //FIXME Play-JSON doesn't support OffsetTime
      //benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME jackson serializes OffsetDateTime as array of numbers
      //toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      //FIXME Play-JSON doesn't support OffsetTime
      //toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}