package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BigDecimalBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new BigDecimalBenchmark {
    setup()
  }
  
  "BigDecimalBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.sourceObj
      benchmark.readCirce() shouldBe benchmark.sourceObj
      //FIXME: dsl-json cannot find decoder for BigDecimal
      //benchmark.readDslJsonJava() shouldBe benchmark.sourceObj
      benchmark.readJacksonScala() shouldBe benchmark.sourceObj
      benchmark.readJsoniterScala() shouldBe benchmark.sourceObj
      benchmark.readPlayJson() shouldBe benchmark.sourceObj
      //FIXME: uPickle parses BigInt from JSON strings only
      //benchmark.readUPickle() shouldBe benchmark.sourceObj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: dsl-json cannot find encoder for BigDecimal
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