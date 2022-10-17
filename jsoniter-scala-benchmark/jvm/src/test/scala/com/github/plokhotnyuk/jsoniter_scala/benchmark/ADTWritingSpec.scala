package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ADTWritingSpec extends BenchmarkSpecBase {
  def benchmark: ADTWriting = new ADTWriting {
    setup()
  }

  "ADTWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString1
      toString(b.borer()) shouldBe b.jsonString1
      toString(b.circe()) shouldBe b.jsonString2
      toString(b.circeJsoniter()) shouldBe b.jsonString2
      toString(b.jacksonScala()) shouldBe b.jsonString1
      //FIXME: json4s.jackson doesn't serialize type hints
      //toString(b.json4sJackson()) shouldBe b.jsonString1
      //FIXME: json4s.native doesn't serialize type hints
      //toString(b.json4sNative()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString1
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.playJson()) shouldBe b.jsonString1
      toString(b.playJsonJsoniter()) shouldBe b.jsonString1
      toString(b.smithy4sJson()) shouldBe b.jsonString1
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