package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfLocalDateTimesReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfLocalDateTimesReading {
    setup()
  }
  
  "ArrayOfLocalDateTimesReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}