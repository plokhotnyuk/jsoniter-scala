package com.github.plokhotnyuk.jsoniter_scala.benchmark

class TwitterAPIWritingSpec extends BenchmarkSpecBase {
  def benchmark: TwitterAPIWriting = new TwitterAPIWriting {
    setup()
  }

  "TwitterAPIWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.compactJsonString1
      toString(b.borer()) shouldBe b.compactJsonString1
      toString(b.circe()) shouldBe b.compactJsonString1
      toString(b.circeJsoniter()) shouldBe b.compactJsonString1
      toString(b.jsoniterScala()) shouldBe b.compactJsonString1
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.compactJsonString1
      toString(b.playJson()) shouldBe b.compactJsonString1
      toString(b.playJsonJsoniter()) shouldBe b.compactJsonString1
      toString(b.smithy4sJson()) shouldBe b.compactJsonString1
      toString(b.uPickle()) shouldBe b.compactJsonString1
      //FIXME: zio-json skips serialization of the `"description": {}` field
      //toString(b.zioJson()) shouldBe b.compactJsonString1
      toString(b.zioSchemaJson()) shouldBe b.compactJsonString1
    }
  }
}