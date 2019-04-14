package com.github.plokhotnyuk.jsoniter_scala.benchmark

class PrimitivesBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new PrimitivesBenchmark
  
  "PrimitivesBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readBorerJson() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for com.github.plokhotnyuk.jsoniter_scala.benchmark.Primitives
      //benchmark.readDslJsonScala() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readSprayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString1
      toString(benchmark.writeBorerJson()) shouldBe benchmark.jsonString1
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString1
      //FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for com.github.plokhotnyuk.jsoniter_scala.benchmark.Primitives
      //toString(benchmark.writeDslJsonScala()) shouldBe benchmark.jsonString1
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString1
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString1
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString1
      toString(benchmark.writeSprayJson()) shouldBe benchmark.jsonString2
      toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString1
    }
  }
}