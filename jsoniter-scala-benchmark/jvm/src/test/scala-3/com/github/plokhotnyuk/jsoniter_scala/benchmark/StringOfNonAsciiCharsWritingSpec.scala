package com.github.plokhotnyuk.jsoniter_scala.benchmark

class StringOfNonAsciiCharsWritingSpec extends BenchmarkSpecBase {
  def benchmark: StringOfNonAsciiCharsWriting = new StringOfNonAsciiCharsWriting {
    setup()
  }

  "StringOfNonAsciiCharsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.json4sJackson()) shouldBe b.jsonString
      //FIXME: json4s.native writes escaped codes for some characters instead of UTF-8 bytes
      //toString(b.json4sNative()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString
    }
  }
}