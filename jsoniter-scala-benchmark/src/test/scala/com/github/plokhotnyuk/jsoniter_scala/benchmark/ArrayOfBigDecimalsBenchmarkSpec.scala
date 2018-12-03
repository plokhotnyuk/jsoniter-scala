package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfBigDecimalsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBigDecimalsBenchmark {
    setup()
  }
  
  "ArrayOfBigDecimalsBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.sourceObj
      benchmark.readCirce() shouldBe benchmark.sourceObj
      //FIXME: dsl-json cannot find decoder for array of BigDecimal
      //benchmark.readDslJsonJava() shouldBe benchmark.sourceObj
      benchmark.readJacksonScala() shouldBe benchmark.sourceObj
      benchmark.readJsoniterScala() shouldBe benchmark.sourceObj
      benchmark.readPlayJson() shouldBe benchmark.sourceObj
      //FIXME: uPickle parses BigDecimal from JSON strings only
      //benchmark.readUPickle() shouldBe benchmark.sourceObj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: dsl-json cannot find encoder for array of BigDecimal
      //toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle serializes BigDecimal to JSON string
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}