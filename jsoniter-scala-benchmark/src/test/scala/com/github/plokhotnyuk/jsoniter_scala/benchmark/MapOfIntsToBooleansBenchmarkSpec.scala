package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MapOfIntsToBooleansBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new MapOfIntsToBooleansBenchmark {
    setup()
  }
  
  "MapOfIntsToBooleansBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readDslJsonScala() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: Spray-JSON throws spray.json.DeserializationException: Expected Int as JsNumber, but got "-1"
      //benchmark.readSprayJson() shouldBe benchmark.obj
      //FIXME: uPickle parses maps from JSON arrays only
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeDslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: Spray-JSON throws spray.json.SerializationException: Map key must be formatted as JsString, not '-130530'
      //toString(benchmark.writeSprayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle serializes maps as JSON arrays
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}