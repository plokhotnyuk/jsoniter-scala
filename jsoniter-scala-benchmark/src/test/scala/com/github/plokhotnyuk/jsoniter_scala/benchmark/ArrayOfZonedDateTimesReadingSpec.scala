package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfZonedDateTimesReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfZonedDateTimesReading {
    setup()
  }
  
  "ArrayOfZonedDateTimesReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      //FIXME: DSL-JSON does not parse preferred timezone
      //benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}