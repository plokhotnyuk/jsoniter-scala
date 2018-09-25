package com.github.plokhotnyuk.jsoniter_scala.macros

class GoogleMapsAPIBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new GoogleMapsAPIBenchmark
  
  "GoogleMapsAPIBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: DSL-JSON throws java.lang.IllegalArgumentException
      //benchmark.readDslJsonJava() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeCirce()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeDslJsonJava()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeJacksonScala()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeJsoniterScala()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writePlayJson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeUPickle()) shouldBe GoogleMapsAPI.compactJsonString
    }
  }
}