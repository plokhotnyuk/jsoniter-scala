package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new OpenRTBReading
  
  "OpenRTBReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borerJson() shouldBe benchmark.obj
      //FIXME: circe throws DecodingFailure(Attempt to decode value on failed cursor, List(DownField(mimes), DownField(banner), DownArray, DownField(imp)))
      //benchmark.circe() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      //FIXME: Spray-JSON throws spray.json.DeserializationException: Object is missing required member 'mimes'
      //benchmark.sprayJson() shouldBe benchmark.obj
    }
  }
}