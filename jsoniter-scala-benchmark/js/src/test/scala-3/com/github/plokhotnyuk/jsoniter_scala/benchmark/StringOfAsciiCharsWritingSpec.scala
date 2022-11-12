package com.github.plokhotnyuk.jsoniter_scala.benchmark

class StringOfAsciiCharsWritingSpec extends BenchmarkSpecBase {
  def benchmark: StringOfAsciiCharsWriting = new StringOfAsciiCharsWriting {
    setup()
  }

  "StringOfAsciiCharsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
      toString(b.zioJson()) shouldBe b.jsonString
    }
  }
}