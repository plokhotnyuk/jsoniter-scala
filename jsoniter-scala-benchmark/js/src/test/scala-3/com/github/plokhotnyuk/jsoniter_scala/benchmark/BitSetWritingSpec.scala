package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BitSetWritingSpec extends BenchmarkSpecBase {
  def benchmark: BitSetWriting = new BitSetWriting {
    setup()
  }

  "BitSetWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
    }
  }
}