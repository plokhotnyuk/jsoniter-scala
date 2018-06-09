package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfBigDecimalsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBigDecimalsBenchmark {
    setup()
  }
  
  "ArrayOfBigDecimalsBenchmark" should {
    "deserialize properly" in {
      //FIXME: AVSystem GenCodec parses BigDecimal from JSON string only
      //benchmark.readAVSystemGenCodec().deep shouldBe benchmark.sourceObj.deep
      benchmark.readCirce().deep shouldBe benchmark.sourceObj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.sourceObj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.sourceObj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.sourceObj.deep
      //FIXME: uPickle parses BigDecimal from JSON string only
      //benchmark.readUPickle().deep shouldBe benchmark.sourceObj.deep
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