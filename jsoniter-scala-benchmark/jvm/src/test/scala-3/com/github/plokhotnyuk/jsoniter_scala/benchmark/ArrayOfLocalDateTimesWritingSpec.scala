package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfLocalDateTimesWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfLocalDateTimesWriting = new ArrayOfLocalDateTimesWriting {
    setup()
  }

  "ArrayOfLocalDateTimesWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.json4sJackson()) shouldBe b.jsonString
      toString(b.json4sNative()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString
    }
  }
}