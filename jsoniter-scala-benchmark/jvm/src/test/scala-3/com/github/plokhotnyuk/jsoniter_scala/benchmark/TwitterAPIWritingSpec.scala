package com.github.plokhotnyuk.jsoniter_scala.benchmark

class TwitterAPIWritingSpec extends BenchmarkSpecBase {
  def benchmark: TwitterAPIWriting = new TwitterAPIWriting {
    setup()
  }

  "TwitterAPIWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.compactJsonString
      toString(b.circe()) shouldBe b.compactJsonString
      toString(b.circeJsoniter()) shouldBe b.compactJsonString
      toString(b.jacksonScala()) shouldBe b.compactJsonString
      //FIXME: json4s.jackson serializes empty collections
      //toString(b.json4sJackson()) shouldBe b.compactJsonString
      //FIXME: json4s.native serializes empty collections
      //toString(b.json4sNative()) shouldBe b.compactJsonString
      toString(b.jsoniterScala()) shouldBe b.compactJsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.compactJsonString
      toString(b.playJson()) shouldBe b.compactJsonString
      toString(b.smithy4sJson()) shouldBe b.compactJsonString
      //FIXME: Spray-JSON serializes empty collections
      //toString(b.sprayJson()) shouldBe b.compactJsonString
      toString(b.uPickle()) shouldBe b.compactJsonString
      toString(b.weePickle()) shouldBe b.compactJsonString
      //FIXME: Zio-JSON serializes empty collections
      //toString(b.zioJson()) shouldBe b.compactJsonString
    }
  }
}