package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableBitSetBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new MutableBitSetBenchmark {
    setup()
  }
  
  "MutableBitSetBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Circe doesn't support parsing of bitsets
      //benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: DSL-JSON throws scala.collection.mutable.HashSet cannot be cast to scala.collection.mutable.BitSet
      //benchmark.readDslJsonScala() shouldBe benchmark.obj
      //FIXME: Jackson throws java.lang.IllegalArgumentException: Need exactly 1 type parameter for collection like types (scala.collection.immutable.BitSet)
      //benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: uPickle doesn't support mutable bitsets
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Circe doesn't support writing of bitsets
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeDslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle doesn't support mutable bitsets
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}