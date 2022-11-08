package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfBigIntsWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfBigIntsWriting = new ArrayOfBigIntsWriting {
    setup()
  }

  "ArrayOfBigIntsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
    }
  }
}