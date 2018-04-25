package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfDoublesBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfDoublesBenchmark {
    setup()
  }
  
  "ArrayOfDoublesBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      benchmark.readDslJsonJava().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: dsl-json serialize doubles in a plain representation
      //toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      //FIXME: Play serializes doubles in different format than toString: 0.0 as 0, 7.0687002407403325E18 as 7068700240740332500
      //toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}