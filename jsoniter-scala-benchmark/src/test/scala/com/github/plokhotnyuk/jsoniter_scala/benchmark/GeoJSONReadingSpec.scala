package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GeoJSONReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new GeoJSONReading
  
  "GeoJSONReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Borer throws io.bullet.borer.Borer$Error$InvalidInputData: Expected Map for decoding an instance of type `com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON.SimpleGeoJSON` but got Start of unbounded Map (input position 41)
      //benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}