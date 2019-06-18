package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfDoublesReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfDoublesReading {
    setup()
  }
  
  "ArrayOfDoublesReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      //FIXME: Jsoniter Java cannot parse some numbers like 5.9823526 precisely
      //benchmark.jsoniterJava() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.scalikeJackson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}