package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MapOfIntsToBooleansReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new MapOfIntsToBooleansReading {
    setup()
  }
  
  "MapOfIntsToBooleansReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      //FIXME: ScalikeJackson returns map of string to boolean instead of map of int to boolean
      //benchmark.scalikeJackson() shouldBe benchmark.obj
      //FIXME: Spray-JSON throws spray.json.DeserializationException: Expected Int as JsNumber, but got "-1"
      //benchmark.sprayJson() shouldBe benchmark.obj
      //FIXME: uPickle parses maps from JSON arrays only
      //benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}