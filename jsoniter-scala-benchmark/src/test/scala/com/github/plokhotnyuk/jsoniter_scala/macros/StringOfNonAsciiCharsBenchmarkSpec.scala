package com.github.plokhotnyuk.jsoniter_scala.macros

class StringOfNonAsciiCharsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new StringOfNonAsciiCharsBenchmark {
    setup()
  }
  
  "StringOfNonAsciiCharsBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readDslJsonJava() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle serializes escaped chars instead of UTF-8
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}