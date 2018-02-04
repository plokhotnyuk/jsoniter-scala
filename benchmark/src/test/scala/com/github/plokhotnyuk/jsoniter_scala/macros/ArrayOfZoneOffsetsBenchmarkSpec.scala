package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfZoneOffsetsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArrayOfZoneOffsetsBenchmark
  
  "ArrayOfZoneOffsetsBenchmark" should {
    "deserialize properly" in {
      //FIXME Circe require custom decoder
      //benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      //FIXME Play json require custom format
      //benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      //FIXME Circe require custom encoder
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      //FIXME Play json require custom format
      //toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}