package com.github.plokhotnyuk.jsoniter_scala.benchmark

class TwitterAPIWritingSpec extends BenchmarkSpecBase {
  def benchmark: TwitterAPIWriting = new TwitterAPIWriting {
    setup()
  }

  "TwitterAPIWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.compactJsonString1
      toString(b.circe()) shouldBe b.compactJsonString1
      toString(b.circeJsoniter()) shouldBe b.compactJsonString1
      toString(b.jacksonScala()) shouldBe b.compactJsonString1
      // FIXME: json4s.jackson serializes empty collections
      //toString(b.json4sJackson()) shouldBe b.compactJsonString1
      // FIXME: json4s.native serializes empty collections
      //toString(b.json4sNative()) shouldBe b.compactJsonString1
      toString(b.jsoniterScala()) shouldBe b.compactJsonString1
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.compactJsonString1
      toString(b.playJson()) shouldBe b.compactJsonString1
      toString(b.playJsonJsoniter()) shouldBe b.compactJsonString1
      toString(b.smithy4sJson()) shouldBe b.compactJsonString1
      toString(b.sprayJson()) shouldBe b.compactJsonString2
      toString(b.uPickle()) shouldBe b.compactJsonString1
      toString(b.weePickle()) shouldBe b.compactJsonString1
      toString(b.zioBlocks()) shouldBe b.compactJsonString1
      toString(b.zioJson()) shouldBe b.compactJsonString1
      toString(b.zioSchemaJson()) shouldBe b.compactJsonString1
    }
  }
}