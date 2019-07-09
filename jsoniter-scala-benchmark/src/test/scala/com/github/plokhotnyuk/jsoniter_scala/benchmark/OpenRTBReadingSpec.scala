package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new OpenRTBReading
  
  "OpenRTBReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      //FIXME: circe throws DecodingFailure(Attempt to decode value on failed cursor, List(DownField(mimes), DownField(banner), DownArray, DownField(imp)))
      //benchmark.circe() shouldBe benchmark.obj
      //FIXME: Jackson doesn't parse default values of missing fields
      //benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      //FIXME: Spray-JSON doesn't support product types with more than 22 fields
      //benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}