package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new GoogleMapsAPIBenchmark
  
  "GoogleMapsAPIBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readBorerJson() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readDslJsonScala() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readSprayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeBorerJson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeCirce()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeDslJsonScala()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeJacksonScala()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeJsoniterScala()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writePlayJson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeSprayJson()) shouldBe GoogleMapsAPI.compactJsonString
      toString(benchmark.writeUPickle()) shouldBe GoogleMapsAPI.compactJsonString
    }
    "pretty print properly" in {
      toString(benchmark.prettyPrintAVSystemGenCodec()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.prettyPrintCirce()) shouldBe GoogleMapsAPI.jsonString1
      //FIXME: DSL-JSON doesn't support pretty printing
      //toString(benchmark.prettyPrintDslJsonScala()) shouldBe GoogleMapsAPI.jsonString1
      toString(benchmark.prettyPrintJacksonScala()) shouldBe GoogleMapsAPI.jsonString1
      toString(benchmark.prettyPrintJsoniterScala()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.preallocatedBuf, 0, benchmark.prettyPrintJsoniterScalaPrealloc()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.prettyPrintPlayJson()) shouldBe GoogleMapsAPI.jsonString1
      toString(benchmark.prettyPrintSprayJson()) shouldBe GoogleMapsAPI.jsonString2
      toString(benchmark.prettyPrintUPickle()) shouldBe GoogleMapsAPI.jsonString2
    }
  }
}