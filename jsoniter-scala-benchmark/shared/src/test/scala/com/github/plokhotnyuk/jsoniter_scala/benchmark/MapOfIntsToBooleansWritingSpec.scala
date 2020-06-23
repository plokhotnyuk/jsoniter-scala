package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MapOfIntsToBooleansWritingSpec extends BenchmarkSpecBase {
  val benchmark = new MapOfIntsToBooleansWriting {
    setup()
  }
  
  "MapOfIntsToBooleansWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      //FIXME: Spray-JSON throws spray.json.SerializationException: Map key must be formatted as JsString, not '-130530'
      //toString(benchmark.sprayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle serializes maps as JSON arrays
      //toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}