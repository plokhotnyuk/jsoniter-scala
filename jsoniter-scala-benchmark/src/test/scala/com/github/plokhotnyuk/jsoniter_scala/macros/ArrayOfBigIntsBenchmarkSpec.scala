package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfBigIntsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBigIntsBenchmark {
    setup()
  }
  
  "ArrayOfBigIntsBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: dsl-json cannot find parser for array of BigInt
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