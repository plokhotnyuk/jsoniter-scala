package com.github.plokhotnyuk.jsoniter_scala.benchmark

class IntMapOfBooleansBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new IntMapOfBooleansBenchmark {
    setup()
  }
  
  "IntMapOfBooleansBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Circe doesn't support IntMap
      //benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for scala.collection.immutable.IntMap[Boolean]
      //benchmark.readDslJsonScala() shouldBe benchmark.obj
      //FIXME: Jackson throws java.lang.IllegalArgumentException: Need exactly 2 type parameters for map like types (scala.collection.immutable.IntMap)
      //benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: uPickle doesn't support IntMap
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Circe doesn't support IntMap
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: DSL-JSON throws java.lang.ClassCastException: scala.Tuple2 cannot be cast to java.lang.Boolean
      //toString(benchmark.writeDslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle doesn't support IntMap
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}