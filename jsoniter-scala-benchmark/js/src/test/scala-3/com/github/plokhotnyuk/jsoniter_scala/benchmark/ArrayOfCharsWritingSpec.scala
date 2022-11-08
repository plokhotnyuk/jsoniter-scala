package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfCharsWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfCharsWriting = new ArrayOfCharsWriting {
    setup()
  }

  "ArrayOfCharsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
    }
  }
}