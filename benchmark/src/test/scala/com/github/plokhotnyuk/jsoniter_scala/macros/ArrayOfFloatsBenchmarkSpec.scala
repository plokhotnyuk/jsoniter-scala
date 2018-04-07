package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfFloatsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArrayOfFloatsBenchmark
  
  "ArrayOfFloatsBenchmark" should {
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
      //FIXME: Play-JSON serialize double values instead of float
      //toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}