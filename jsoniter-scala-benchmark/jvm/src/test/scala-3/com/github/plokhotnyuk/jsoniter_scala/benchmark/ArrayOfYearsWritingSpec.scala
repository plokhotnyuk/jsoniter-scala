package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfYearsWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfYearsWriting = new ArrayOfYearsWriting {
    setup()
  }

  "ArrayOfYearsWriting" should {
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