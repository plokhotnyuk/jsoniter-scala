package com.github.plokhotnyuk.jsoniter_scala.macros

class MutableBitSetBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new MutableBitSetBenchmark {
    setup()
  }
  
  "MutableBitSetBenchmark" should {
    "deserialize properly" in {
      //FIXME: Circe doesn't support parsing of bitsets
      //benchmark.readCirce() shouldBe benchmark.bitSetsObj
      //FIXME: Jackson throws java.lang.IllegalArgumentException: Need exactly 1 type parameter for collection like types (scala.collection.immutable.BitSet)
      //benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: Circe doesn't support writing of bitsets
      //toString(benchmark.writeCirce()) shouldBe benchmark.bitSetsJsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}