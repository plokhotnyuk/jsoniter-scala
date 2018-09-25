package com.github.plokhotnyuk.jsoniter_scala.macros

class MutableLongMapOfBooleansBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new MutableLongMapOfBooleansBenchmark {
    setup()
  }
  
  "MutableLongMapOfBooleansBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Circe doesn't support mutable.LongMap
      //benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: Jackson throws Need exactly 2 type parameters for map like types (scala.collection.mutable.LongMap)
      //benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: uPickle doesn't support mutable.LongMap
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Circe doesn't support mutable.LongMap
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle doesn't support mutable.LongMap
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}