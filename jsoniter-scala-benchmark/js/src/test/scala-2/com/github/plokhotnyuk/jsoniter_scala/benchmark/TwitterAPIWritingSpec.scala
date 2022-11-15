package com.github.plokhotnyuk.jsoniter_scala.benchmark

class TwitterAPIWritingSpec extends BenchmarkSpecBase {
  def benchmark: TwitterAPIWriting = new TwitterAPIWriting {
    setup()
  }

  "TwitterAPIWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.compactJsonString
      toString(b.borer()) shouldBe b.compactJsonString
      toString(b.circe()) shouldBe b.compactJsonString
      toString(b.circeJsoniter()) shouldBe b.compactJsonString
      toString(b.jsoniterScala()) shouldBe b.compactJsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.compactJsonString
      toString(b.playJson()) shouldBe b.compactJsonString
      toString(b.playJsonJsoniter()) shouldBe b.compactJsonString
      toString(b.smithy4sJson()) shouldBe b.compactJsonString
      toString(b.uPickle()) shouldBe b.compactJsonString
      //FIXME: Zio-JSON serializes empty collections
      //toString(b.zioJson()) shouldBe b.compactJsonString
    }
  }
}