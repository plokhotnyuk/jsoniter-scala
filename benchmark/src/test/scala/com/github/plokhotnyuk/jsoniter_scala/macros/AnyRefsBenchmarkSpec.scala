package com.github.plokhotnyuk.jsoniter_scala.macros

class AnyRefsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new AnyRefsBenchmark
  
  "AnyRefsBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readDslJsonJava() shouldBe benchmark.obj
      benchmark.readDslJsonScala() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeDslJsonJavaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writeDslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeDslJsonScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}