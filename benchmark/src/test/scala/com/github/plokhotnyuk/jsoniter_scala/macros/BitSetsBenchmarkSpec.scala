package com.github.plokhotnyuk.jsoniter_scala.macros

class BitSetsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new BitSetsBenchmark
  
  "BitSetsBenchmark" should {
    "deserialize properly" in {
      //FIXME: Circe doesn't support parsing of bitsets
      //benchmark.readCirce() shouldBe benchmark.bitSetsObj
      benchmark.readJacksonScala() shouldBe benchmark.obj
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