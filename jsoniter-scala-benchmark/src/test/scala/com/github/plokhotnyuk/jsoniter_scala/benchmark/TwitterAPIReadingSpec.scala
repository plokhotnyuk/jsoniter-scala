package com.github.plokhotnyuk.jsoniter_scala.benchmark

class TwitterAPIReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new TwitterAPIReading
  
  "TwitterAPIReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Borer throws io.bullet.borer.Borer$Error$InvalidInputData: Expected String or Text Bytes but got Null (input position 994)
      //benchmark.borerJson() shouldBe benchmark.obj
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