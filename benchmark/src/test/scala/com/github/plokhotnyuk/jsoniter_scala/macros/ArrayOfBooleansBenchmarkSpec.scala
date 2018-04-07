package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfBooleansBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArrayOfBooleansBenchmark
  
  "ArrayOfBooleansBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readDslJsonJava().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      val writer = benchmark.writeDslJsonJavaPrealloc()
      toString(writer.getByteBuffer, writer.size()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}