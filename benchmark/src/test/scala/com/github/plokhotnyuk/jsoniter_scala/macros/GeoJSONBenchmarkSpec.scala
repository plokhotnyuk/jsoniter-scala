package com.github.plokhotnyuk.jsoniter_scala.macros

class GeoJSONBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new GeoJSONBenchmark
  
  "GoogleMapsAPIBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      //FIXME: Play-JSON doesn't support Tuple2?
      //benchmark.readPlayJson() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe GeoJSON.jsonString2
      toString(benchmark.writeJacksonScala()) shouldBe GeoJSON.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe GeoJSON.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe GeoJSON.jsonString
      //FIXME: Play-JSON doesn't support Tuple2?
      //toString(benchmark.writePlayJson()) shouldBe GeoJSON.jsonString
    }
  }
}