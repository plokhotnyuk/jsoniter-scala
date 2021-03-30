package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MapOfIntsToBooleansReadingSpec extends BenchmarkSpecBase {
  def benchmark: MapOfIntsToBooleansReading = new MapOfIntsToBooleansReading {
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
      benchmark.playJsonJsoniter() shouldBe benchmark.obj
      //FIXME: Spray-JSON throws spray.json.DeserializationException: Expected Int as JsNumber, but got "-1"
      //benchmark.sprayJson() shouldBe benchmark.obj
      //FIXME: uPickle parses maps from JSON arrays only
      //benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
      benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(0) = 'x'.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.dslJsonScala())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.weePickle())
      intercept[Throwable](b.zioJson())
    }
  }
}