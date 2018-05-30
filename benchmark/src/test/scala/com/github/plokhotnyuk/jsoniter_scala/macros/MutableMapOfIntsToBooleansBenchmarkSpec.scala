package com.github.plokhotnyuk.jsoniter_scala.macros

class MutableMapOfIntsToBooleansBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new MutableMapOfIntsToBooleansBenchmark {
    setup()
  }
  
  "MutableMapOfIntsToBooleansBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: uPickle doesn't support mutable maps
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: Circe changes order of entries
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle doesn't support mutable maps
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}