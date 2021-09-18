package com.github.plokhotnyuk.jsoniter_scala.benchmark

class TwitterAPIWritingSpec extends BenchmarkSpecBase {
  def benchmark = new TwitterAPIWriting

  "TwitterAPIWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.compactJsonString
      toString(b.borer()) shouldBe b.compactJsonString
      toString(b.circe()) shouldBe b.compactJsonString
      //FIXME: DSL-JSON serializes empty collections
      //toString(b.dslJsonScala()) shouldBe b.compactJsonString
      toString(b.jacksonScala()) shouldBe b.compactJsonString
      toString(b.jsoniterScala()) shouldBe b.compactJsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.compactJsonString
      //FIXME: Play-JSON serializes empty collections
      //toString(b.playJson()) shouldBe b.compactJsonString
      //FIXME: Spray-JSON serializes empty collections
      //toString(b.sprayJson()) shouldBe b.compactJsonString
      toString(b.uPickle()) shouldBe b.compactJsonString
      toString(b.weePickle()) shouldBe b.compactJsonString
      //FIXME: Zio-JSON serializes empty collections
      //toString(b.zioJson()) shouldBe b.compactJsonString
    }
  }
}