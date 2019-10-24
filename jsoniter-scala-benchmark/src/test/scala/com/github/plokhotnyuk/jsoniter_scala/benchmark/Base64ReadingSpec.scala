package com.github.plokhotnyuk.jsoniter_scala.benchmark

class Base64ReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new Base64Reading {
    setup()
  }
  
  "Base64Reading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
    }
  }
}