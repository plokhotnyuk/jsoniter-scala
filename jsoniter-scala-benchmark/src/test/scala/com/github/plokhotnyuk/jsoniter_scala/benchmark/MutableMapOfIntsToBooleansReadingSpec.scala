package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableMapOfIntsToBooleansReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new MutableMapOfIntsToBooleansReading {
    setup()
  }
  
  "MutableMapOfIntsToBooleansReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      //FIXME: uPickle doesn't support mutable maps
      //benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}