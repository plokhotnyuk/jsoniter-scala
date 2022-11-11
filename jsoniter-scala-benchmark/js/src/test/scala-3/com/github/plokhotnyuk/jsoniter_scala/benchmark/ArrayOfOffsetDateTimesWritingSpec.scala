package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfOffsetDateTimesWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfOffsetDateTimesWriting = new ArrayOfOffsetDateTimesWriting {
    setup()
  }

  "ArrayOfOffsetDateTimesWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
    }
  }
}