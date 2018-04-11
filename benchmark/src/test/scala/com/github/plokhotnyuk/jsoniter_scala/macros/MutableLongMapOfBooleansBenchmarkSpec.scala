package com.github.plokhotnyuk.jsoniter_scala.macros

class MutableLongMapOfBooleansBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new MutableLongMapOfBooleansBenchmark
  
  "MutableLongMapOfBooleansBenchmark" should {
    "deserialize properly" in {
      //FIXME: Circe doesn't support mutable.LongMap
      //benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: Jackson throws Need exactly 2 type parameters for map like types (scala.collection.mutable.LongMap)
      //benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: Circe doesn't support mutable.LongMap
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}