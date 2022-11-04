package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MapOfIntsToBooleansWritingSpec extends BenchmarkSpecBase {
  def benchmark: MapOfIntsToBooleansWriting = new MapOfIntsToBooleansWriting {
    setup()
  }

  "MapOfIntsToBooleansWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.json4sJackson()) shouldBe b.jsonString
      toString(b.json4sNative()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString
    }
  }
}