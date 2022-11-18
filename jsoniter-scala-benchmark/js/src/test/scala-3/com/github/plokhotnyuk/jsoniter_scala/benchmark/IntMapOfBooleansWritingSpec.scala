package com.github.plokhotnyuk.jsoniter_scala.benchmark

class IntMapOfBooleansWritingSpec extends BenchmarkSpecBase {
  def benchmark: IntMapOfBooleansWriting = new IntMapOfBooleansWriting {
    setup()
  }

  "IntMapOfBooleansWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      //FIXME: uPickle doesn't support IntMap
      //toString(b.uPickle()) shouldBe b.jsonString
    }
  }
}