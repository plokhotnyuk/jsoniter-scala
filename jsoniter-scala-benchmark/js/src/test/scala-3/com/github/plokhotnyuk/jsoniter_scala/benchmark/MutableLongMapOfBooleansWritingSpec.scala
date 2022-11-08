package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableLongMapOfBooleansWritingSpec extends BenchmarkSpecBase {
  def benchmark: MutableLongMapOfBooleansWriting = new MutableLongMapOfBooleansWriting {
    setup()
  }

  "MutableLongMapOfBooleansWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
    }
  }
}