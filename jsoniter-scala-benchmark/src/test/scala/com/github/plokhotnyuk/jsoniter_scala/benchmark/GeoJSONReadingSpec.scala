package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GeoJSONReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new GeoJSONReading
  
  "GeoJSONReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Borer throws: io.bullet.borer.Borer$Error$InvalidInputData: Expected type id key for subtype of `com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON.Geometry` but got String (input position 104)
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