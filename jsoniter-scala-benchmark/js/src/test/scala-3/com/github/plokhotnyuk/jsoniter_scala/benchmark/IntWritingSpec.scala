package com.github.plokhotnyuk.jsoniter_scala.benchmark

class IntWritingSpec extends BenchmarkSpecBase {
  def benchmark: IntWriting = new IntWriting {
    setup()
  }

  "IntWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
    }
  }
}