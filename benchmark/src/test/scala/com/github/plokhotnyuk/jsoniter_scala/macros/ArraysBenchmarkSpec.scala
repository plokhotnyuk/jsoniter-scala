package com.github.plokhotnyuk.jsoniter_scala.macros

class ArraysBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArraysBenchmark
  
  "ArraysBenchmark" should {
    "deserialize properly" in {
      assertArrays(benchmark.readCirce(), benchmark.obj)
      assertArrays(benchmark.readJacksonScala(), benchmark.obj)
      assertArrays(benchmark.readJsoniterScala(), benchmark.obj)
      assertArrays(benchmark.readPlayJson(), benchmark.obj)
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }

  private def assertArrays(parsedObj: Arrays, obj: Arrays): Unit = {
    parsedObj.aa.deep shouldBe obj.aa.deep
    parsedObj.a.deep shouldBe obj.a.deep
  }
}