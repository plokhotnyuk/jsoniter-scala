package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfZoneIdsWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfZoneIdsWriting = new ArrayOfZoneIdsWriting {
    setup()
  }

  "ArrayOfZoneIdsWriting" should {
    "serialize properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
    }
  }
}