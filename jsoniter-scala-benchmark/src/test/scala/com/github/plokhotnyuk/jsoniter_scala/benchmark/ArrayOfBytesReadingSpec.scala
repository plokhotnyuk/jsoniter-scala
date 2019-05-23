package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfBytesReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBytesReading {
    setup()
  }
  
  "ArrayOfBytesReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Borer throws io.bullet.borer.Borer$Error$InvalidInputData: Expected Bytes but got Start of unbounded Array (input position 0)
      //benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      //FIXME:dsl-json expects a base64 string for the byte array
      //benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterJava() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}