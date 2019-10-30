package com.github.plokhotnyuk.jsoniter_scala.benchmark

class Base16ReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new Base16Reading {
    setup()
  }
  
  "Base16Reading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borerJson() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
    }
  }
}