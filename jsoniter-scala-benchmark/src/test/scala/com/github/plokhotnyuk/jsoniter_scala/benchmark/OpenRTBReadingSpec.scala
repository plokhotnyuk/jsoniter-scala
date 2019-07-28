package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new OpenRTBReading
  
  "OpenRTBReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      //FIXME: Jackson parses nulls in case of missing fields for lists
      //benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      //FIXME: Spray-JSON throws spray.json.DeserializationException: Object is missing required member 'expdir'
      //benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}