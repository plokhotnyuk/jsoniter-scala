package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableBitSetWritingSpec extends BenchmarkSpecBase {
  def benchmark: MutableBitSetWriting = new MutableBitSetWriting {
    setup()
  }

  "MutableBitSetWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
    }
  }
}