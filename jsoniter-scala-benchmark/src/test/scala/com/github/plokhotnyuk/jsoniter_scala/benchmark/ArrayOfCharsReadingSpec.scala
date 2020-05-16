package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfCharsReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfCharsReading {
    setup()
  }
  
  "ArrayOfCharsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
    }
  }
}