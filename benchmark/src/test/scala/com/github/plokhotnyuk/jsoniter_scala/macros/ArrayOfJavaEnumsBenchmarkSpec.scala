package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfJavaEnumsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArrayOfJavaEnumsBenchmark
  
  "ArrayOfJavaEnumsBenchmark" should {
    "deserialize properly" in {
      //FIXME circe doesn't support Java enums
      //benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      //FIXME circe doesn't support Java enums
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}