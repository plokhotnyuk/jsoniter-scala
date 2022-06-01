package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ADTWritingSpec extends BenchmarkSpecBase {
  def benchmark = new ADTWriting

  "ADTWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString1
      toString(b.borer()) shouldBe b.jsonString1
      toString(b.circe()) shouldBe b.jsonString2
      toString(b.circeJsoniter()) shouldBe b.jsonString2
      toString(b.jacksonScala()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString1
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.ninnyJson()) shouldBe b.jsonString3
      toString(b.playJson()) shouldBe b.jsonString1
      toString(b.playJsonJsoniter()) shouldBe b.jsonString1
      toString(b.smithy4s()) shouldBe b.jsonString1
      toString(b.sprayJson()) shouldBe b.jsonString2
      toString(b.uPickle()) shouldBe b.jsonString1
      toString(b.weePickle()) shouldBe b.jsonString1
      toString(b.zioJson()) shouldBe b.jsonString1
    }
    "fail on invalid input" in {
      val b = benchmark
      b.obj = null
      intercept[Throwable](b.sprayJson())
    }
  }
}