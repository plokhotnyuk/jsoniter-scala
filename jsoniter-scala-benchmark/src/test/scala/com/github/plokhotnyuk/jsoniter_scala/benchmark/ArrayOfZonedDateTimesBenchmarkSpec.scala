package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfZonedDateTimesBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfZonedDateTimesBenchmark {
    setup()
  }
  
  "ArrayOfZonedDateTimesBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: DSL-JSON does not parse preferred timezone
      //benchmark.readDslJsonScala() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readSprayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: DSL-JSON does not serialize preferred timezone
      //toString(benchmark.writeDslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      toString(benchmark.writeSprayJson()) shouldBe benchmark.jsonString
      toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}