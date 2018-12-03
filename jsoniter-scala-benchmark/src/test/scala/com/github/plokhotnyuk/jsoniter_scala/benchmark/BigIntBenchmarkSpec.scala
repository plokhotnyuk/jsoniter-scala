package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BigIntBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new BigIntBenchmark {
    setup()
  }
  
  "BigIntBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: dsl-json cannot find decoder for array of BigInt
      //benchmark.readDslJsonJava() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: uPickle parses BigInt from JSON strings only
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Circe uses an engineering decimal notation to serialize BigInt
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: dsl-json cannot find encoder for array of BigInt
      //toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle serializes BigInt to JSON strings
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}