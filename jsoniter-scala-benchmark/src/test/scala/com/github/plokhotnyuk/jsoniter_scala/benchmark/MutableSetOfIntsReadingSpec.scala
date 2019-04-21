package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableSetOfIntsReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new MutableSetOfIntsReading {
    setup()
  }
  
  "MutableSetOfIntsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}