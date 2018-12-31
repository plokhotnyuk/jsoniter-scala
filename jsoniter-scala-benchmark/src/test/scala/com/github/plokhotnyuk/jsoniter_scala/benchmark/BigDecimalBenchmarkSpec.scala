package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BigDecimalBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new BigDecimalBenchmark {
    setup()
  }
  
  "BigDecimalBenchmark" should {
    "deserialize properly" in {
      //FIXME: AVSystem GenCodec: don't know how to tune precision for parsing of BigDecimal values
      //benchmark.readAVSystemGenCodec() shouldBe benchmark.sourceObj
      benchmark.readCirce() shouldBe benchmark.sourceObj
      //FIXME: dsl-json cannot find decoder for BigDecimal
      //benchmark.readDslJsonScala() shouldBe benchmark.sourceObj
      benchmark.readJacksonScala() shouldBe benchmark.sourceObj
      benchmark.readJsoniterScala() shouldBe benchmark.sourceObj
      //FIXME: Play-JSON: don't know how to tune precision for parsing of BigDecimal values
      //benchmark.readPlayJson() shouldBe benchmark.sourceObj
      benchmark.readUPickle() shouldBe benchmark.sourceObj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: dsl-json cannot find encoder for BigDecimal
      //toString(benchmark.writeDslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      //FIXME: Play-JSON serializes BigInt in a scientific representation (as BigDecimal)
      //toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}