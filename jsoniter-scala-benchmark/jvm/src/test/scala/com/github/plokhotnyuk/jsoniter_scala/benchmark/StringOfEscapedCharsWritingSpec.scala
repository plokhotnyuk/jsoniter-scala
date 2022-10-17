package com.github.plokhotnyuk.jsoniter_scala.benchmark

class StringOfEscapedCharsWritingSpec extends BenchmarkSpecBase {
  def benchmark: StringOfEscapedCharsWriting = new StringOfEscapedCharsWriting {
    setup()
  }

  "StringOfEscapedCharsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString1
      toString(b.circe()) shouldBe b.jsonString1
      toString(b.circeJsoniter()) shouldBe b.jsonString1
      toString(b.jacksonScala()) shouldBe b.jsonString2
      //FIXME: json4s.jackson doesn't escape unicode
      //toString(b.json4sJackson()) shouldBe b.jsonString1
      //FIXME: json4s.native doesn't escape unicode
      //toString(b.json4sNative()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString1
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.playJson()) shouldBe b.jsonString2
      toString(b.playJsonJsoniter()) shouldBe b.jsonString1
      toString(b.smithy4sJson()) shouldBe b.jsonString1
      toString(b.uPickle()) shouldBe b.jsonString1
      toString(b.weePickle()) shouldBe b.jsonString2
    }
  }
}