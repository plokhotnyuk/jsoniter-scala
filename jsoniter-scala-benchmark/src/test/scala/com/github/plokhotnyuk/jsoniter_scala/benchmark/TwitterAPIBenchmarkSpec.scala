package com.github.plokhotnyuk.jsoniter_scala.benchmark

class TwitterAPIBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new TwitterAPIBenchmark
  
  "TwitterAPIBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: DSL_JSON throws java.lang.IllegalArgumentException: argument type mismatch
      //benchmark.readDslJsonScala() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readSprayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe TwitterAPI.compactJsonString
      //FIXME: circe serializes empty collections
      //toString(benchmark.writeCirce()) shouldBe TwitterAPI.compactJsonString
      //FIXME: DSL-JSON serializes empty collections
      //toString(benchmark.writeDslJsonScala()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeJacksonScala()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeJsoniterScala()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe TwitterAPI.compactJsonString
      //FIXME: Play-JSON serializes empty collections
      //toString(benchmark.writePlayJson()) shouldBe TwitterAPI.compactJsonString
      //FIXME: Spray-JSON serializes empty collections
      //toString(benchmark.writeSprayJson()) shouldBe TwitterAPI.compactJsonString
      //FIXME: uPickle serializes empty collections
      //toString(benchmark.writeUPickle()) shouldBe TwitterAPI.compactJsonString
    }
  }
}