package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new OpenRTBReading
  
  "OpenRTBReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      //FIXME: Play-JSON requires fields for lists with default values
      //benchmark.playJson() shouldBe benchmark.obj
      //FIXME: Spray-JSON throws spray.json.DeserializationException: Object is missing required member 'expdir'
      //benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
    }
  }
}