package com.github.plokhotnyuk.jsoniter_scala.benchmark

class StringOfEscapedCharsWritingSpec extends BenchmarkSpecBase {
  def benchmark: StringOfEscapedCharsWriting = new StringOfEscapedCharsWriting {
    setup()
  }

  "StringOfEscapedCharsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString2
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString2
      toString(b.uPickle()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString2
    }
  }
}