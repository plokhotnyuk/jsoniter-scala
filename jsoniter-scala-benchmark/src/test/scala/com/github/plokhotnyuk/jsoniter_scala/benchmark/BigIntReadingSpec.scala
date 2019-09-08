package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BigIntReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new BigIntReading {
    setup()
  }
  
  "BigIntReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Borer doesn't allow BigInts longer than 200 decimal digits
      //benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      //FIXME: Play-JSON looses significant digits in BigInt values
      //benchmark.playJson() shouldBe benchmark.obj
      benchmark.scalikeJackson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}