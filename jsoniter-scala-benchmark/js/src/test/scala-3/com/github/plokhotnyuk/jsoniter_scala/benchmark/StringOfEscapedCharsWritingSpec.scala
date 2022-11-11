package com.github.plokhotnyuk.jsoniter_scala.benchmark

class StringOfEscapedCharsWritingSpec extends BenchmarkSpecBase {
  def benchmark: StringOfEscapedCharsWriting = new StringOfEscapedCharsWriting {
    setup()
  }

  "StringOfEscapedCharsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.circe()) shouldBe b.jsonString1
      toString(b.circeJsoniter()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString1
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.smithy4sJson()) shouldBe b.jsonString1
    }
  }
}