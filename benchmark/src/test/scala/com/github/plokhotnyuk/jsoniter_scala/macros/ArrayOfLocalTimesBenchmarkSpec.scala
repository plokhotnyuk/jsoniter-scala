package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfLocalTimesBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArrayOfLocalTimesBenchmark
  
  "ArrayOfLocalTimesBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      //FIXME circe serializes optional seconds
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME jackson serializes LocalTime as array of numbers
      //toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      //FIXME play-json serializes optional seconds
      //toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}