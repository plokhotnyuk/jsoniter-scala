package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfPeriodsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfPeriodsBenchmark {
    setup()
  }
  
  "ArrayOfPeriodsBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec().deep shouldBe benchmark.obj.deep
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
      benchmark.readUPickle().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}