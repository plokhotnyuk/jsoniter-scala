package com.github.plokhotnyuk.jsoniter_scala.benchmark

class NestedStructsReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new NestedStructsReading {
    setup()
  }
  
  "NestedStructsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Borer doesn't support recusive structures, see https://github.com/sirthias/borer/issues/28
      //benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.scalikeJackson() shouldBe benchmark.obj
      //benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}