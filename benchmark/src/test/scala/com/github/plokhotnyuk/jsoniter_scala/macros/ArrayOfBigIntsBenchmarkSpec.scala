package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfBigIntsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBigIntsBenchmark {
    setup()
  }
  
  "ArrayOfBigIntsBenchmark" should {
    "deserialize properly" in {
      //FIXME: AVSystem GenCodec parses BigInt from JSON string only
      //benchmark.readAVSystemGenCodec().deep shouldBe benchmark.obj.deep
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      //FIXME: dsl-json cannot find parser for array of BigInt
      //benchmark.readDslJsonJava().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
      //FIXME: uPickle parses BigInt from JSON string only
      //benchmark.readUPickle().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      //FIXME: AVSystem GenCodec serializes BigDecimal to JSON string
      //toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Circe uses an engineering decimal notation to serialize BigInt
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: dsl-json cannot find serializer for array of BigInt
      //toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      //FIXME: Play-json uses BigDecimal with engineering decimal representation to serialize numbers
      //toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle serializes BigInt to JSON string
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}