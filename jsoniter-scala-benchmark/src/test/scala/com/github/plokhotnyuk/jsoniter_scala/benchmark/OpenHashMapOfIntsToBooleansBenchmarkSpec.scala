package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenHashMapOfIntsToBooleansBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new OpenHashMapOfIntsToBooleansBenchmark {
    setup()
  }
  
  "OpenHashMapOfIntsToBooleansBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Circe doesn't support parsing of OpenHashMap
      //benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: Jackson cannot deserialize OpenHashMap and throws java.lang.ClassCastException: scala.collection.mutable.HashMap cannot be cast to scala.collection.mutable.OpenHashMap
      //benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: uPickle doesn't support parsing of OpenHashMap
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Circe doesn't support serialization of OpenHashMap
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle doesn't support serialization of OpenHashMap
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}