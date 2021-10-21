package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableBitSetWritingSpec extends BenchmarkSpecBase {
  def benchmark: MutableBitSetWriting = new MutableBitSetWriting {
    setup()
  }

  "MutableBitSetWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.dslJsonScala()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      toString(b.playJsonJsoniter()) shouldBe b.jsonString
    }
  }
}