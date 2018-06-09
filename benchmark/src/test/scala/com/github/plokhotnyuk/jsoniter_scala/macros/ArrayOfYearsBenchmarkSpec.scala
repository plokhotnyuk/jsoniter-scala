package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfYearsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfYearsBenchmark {
    setup()
  }
  
  "ArrayOfYearsBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec().deep shouldBe benchmark.obj.deep
      //FIXME: Circe doesn't supports java.time.Year
      //benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
      benchmark.readUPickle().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Circe doesn't supports java.time.Year
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}