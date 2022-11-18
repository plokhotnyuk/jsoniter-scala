package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfZonedDateTimesWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfZonedDateTimesWriting = new ArrayOfZonedDateTimesWriting {
    setup()
  }

  "ArrayOfZonedDateTimesWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
      toString(b.zioJson()) shouldBe b.jsonString
    }
  }
}