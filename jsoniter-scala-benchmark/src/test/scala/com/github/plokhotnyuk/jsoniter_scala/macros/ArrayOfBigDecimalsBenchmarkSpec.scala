package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfBigDecimalsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBigDecimalsBenchmark {
    setup()
  }
  
  "ArrayOfBigDecimalsBenchmark" should {
    "deserialize properly" in {
      //FIXME: AVSystem GenCodec parses BigDecimal from JSON string only
      //benchmark.readAVSystemGenCodec() shouldBe benchmark.sourceObj
      benchmark.readCirce() shouldBe benchmark.sourceObj
      benchmark.readJacksonScala() shouldBe benchmark.sourceObj
      benchmark.readJsoniterScala() shouldBe benchmark.sourceObj
      benchmark.readPlayJson() shouldBe benchmark.sourceObj
      //FIXME: uPickle parses BigDecimal from JSON strings only
      //benchmark.readUPickle() shouldBe benchmark.sourceObj
    }
    "serialize properly" in {
      //FIXME: AVSystem GenCodec serializes BigDecimal to JSON string
      //toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle serializes BigDecimal to JSON string
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}