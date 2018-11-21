package com.github.plokhotnyuk.jsoniter_scala.macros

class GeoJSONBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new GeoJSONBenchmark
  
  "GeoJSONBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: cannot alter uPickle discriminator name and value for ADT
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe GeoJSON.jsonString1
      toString(benchmark.writeCirce()) shouldBe GeoJSON.jsonString2
      toString(benchmark.writeJacksonScala()) shouldBe GeoJSON.jsonString1
      toString(benchmark.writeJsoniterScala()) shouldBe GeoJSON.jsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe GeoJSON.jsonString1
      toString(benchmark.writePlayJson()) shouldBe GeoJSON.jsonString3
      //FIXME: cannot alter uPickle discriminator name and value for ADT
      //toString(benchmark.writeUPickle()) shouldBe GeoJSON.jsonString
    }
  }
}