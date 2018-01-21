package com.github.plokhotnyuk.jsoniter_scala.macros

class StringOfNonAsciiCharsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new StringOfNonAsciiCharsBenchmark
  
  "StringBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      //FIXME: find proper way to parse string value in Play JSON
      //benchmark.readNonAsciiStringPlayJson() shouldBe benchmark.nonAsciiStringObj
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}