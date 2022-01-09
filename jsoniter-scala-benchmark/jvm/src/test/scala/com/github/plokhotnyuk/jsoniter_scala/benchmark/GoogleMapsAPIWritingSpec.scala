package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIWritingSpec extends BenchmarkSpecBase {
  def benchmark = new GoogleMapsAPIWriting

  "GoogleMapsAPIWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.compactJsonString1
      toString(b.borer()) shouldBe b.compactJsonString1
      toString(b.circe()) shouldBe b.compactJsonString1
      toString(b.circeJsoniter()) shouldBe b.compactJsonString1
      toString(b.dslJsonScala()) shouldBe b.compactJsonString1
      toString(b.jacksonScala()) shouldBe b.compactJsonString1
      toString(b.jsoniterScala()) shouldBe b.compactJsonString1
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.compactJsonString1
      toString(b.ninnyJson()) shouldBe b.compactJsonString2
      toString(b.playJson()) shouldBe b.compactJsonString1
      toString(b.playJsonJsoniter()) shouldBe b.compactJsonString1
      toString(b.sprayJson()) shouldBe b.compactJsonString1
      toString(b.uPickle()) shouldBe b.compactJsonString1
      toString(b.weePickle()) shouldBe b.compactJsonString1
      toString(b.zioJson()) shouldBe b.compactJsonString1
    }
  }
}