package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MapOfIntsToBooleansWritingSpec extends BenchmarkSpecBase {
  def benchmark: MapOfIntsToBooleansWriting = new MapOfIntsToBooleansWriting {
    setup()
  }
  
  "MapOfIntsToBooleansWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.dslJsonScala()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      toString(b.playJsonJsoniter()) shouldBe b.jsonString
      //FIXME: Spray-JSON throws spray.json.SerializationException: Map key must be formatted as JsString, not '-130530'
      //toString(b.sprayJson()) shouldBe b.jsonString
      //FIXME: uPickle serializes maps as JSON arrays
      //toString(b.uPickle()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString
      toString(b.zioJson()) shouldBe b.jsonString
    }
  }
}