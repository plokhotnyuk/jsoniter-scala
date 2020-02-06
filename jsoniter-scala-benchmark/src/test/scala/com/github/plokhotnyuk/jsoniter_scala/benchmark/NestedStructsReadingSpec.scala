package com.github.plokhotnyuk.jsoniter_scala.benchmark

class NestedStructsReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new NestedStructsReading {
    setup()
  }
  
  "NestedStructsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Borer throws io.bullet.borer.Borer$Error$Overflow: This JSON parser does not support more than 64 Array/Object nesting levels
      //benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
    }
  }
}