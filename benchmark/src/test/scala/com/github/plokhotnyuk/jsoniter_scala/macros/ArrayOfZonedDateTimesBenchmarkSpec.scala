package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfZonedDateTimesBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArrayOfZonedDateTimesBenchmark
  
  "ArrayOfZonedDateTimesBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      //FIXME jackson parse ZonedDateTime with conversion to Z time zone
      //benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME jackson serializes ZonedDateTime as array of numbers
      //toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}