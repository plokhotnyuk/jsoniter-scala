package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ExtractFieldsReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new ExtractFieldsReading {
    setup()
  }
  
  "ExtractFieldsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
/*
      benchmark.jsoniterScalaIO() shouldBe benchmark.obj
      benchmark.jsoniterScalaNIO() shouldBe benchmark.obj
*/
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.scalaJack() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}