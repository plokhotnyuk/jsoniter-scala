package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ADTWritingSpec extends BenchmarkSpecBase {
  def benchmark: ADTWriting = new ADTWriting {
    setup()
  }

  "ADTWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString1
      toString(b.jacksonScala()) shouldBe b.jsonString1
      toString(b.json4sJackson()) shouldBe b.jsonString1
      toString(b.json4sNative()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString1
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.smithy4sJson()) shouldBe b.jsonString1
      toString(b.weePickle()) shouldBe b.jsonString1
    }
  }
}