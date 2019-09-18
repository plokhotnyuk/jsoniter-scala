package com.github.plokhotnyuk.jsoniter_scala.benchmark

class TwitterAPIReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new TwitterAPIReading
  
  "TwitterAPIReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      //FIXME: ScalikeJackson parses to the sequence of maps instead a sequence of Tweet
      //benchmark.scalikeJackson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
    }
  }
}