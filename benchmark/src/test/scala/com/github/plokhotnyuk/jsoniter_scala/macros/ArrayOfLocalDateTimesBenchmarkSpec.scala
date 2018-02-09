package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfLocalDateTimesBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArrayOfLocalDateTimesBenchmark
  
  "ArrayOfLocalDateTimesBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME Jackson serializes LocalData in format [<year>,<month>,<day>,<hour>,<minute>,<second>]
      //toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}