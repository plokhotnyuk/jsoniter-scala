package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GeoJSONReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new GeoJSONReading
  
  "GeoJSONReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      //FIXME: Play-JSON throws play.api.libs.json.JsResultException with Scala 2.13
      //benchmark.playJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}