package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfEnumADTsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfEnumADTsBenchmark {
    setup()
  }
  
  "ArrayOfEnumsBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Circe doesn't support enum ADT
      //benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Circe doesn't support enum ADT
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}