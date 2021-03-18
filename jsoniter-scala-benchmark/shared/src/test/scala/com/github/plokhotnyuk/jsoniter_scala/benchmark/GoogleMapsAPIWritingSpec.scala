package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIWritingSpec extends BenchmarkSpecBase {
  def benchmark = new GoogleMapsAPIWriting
  
  "GoogleMapsAPIWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.compactJsonString
      toString(b.borer()) shouldBe b.compactJsonString
      toString(b.circe()) shouldBe b.compactJsonString
      toString(b.dslJsonScala()) shouldBe b.compactJsonString
      toString(b.jacksonScala()) shouldBe b.compactJsonString
      toString(b.jsoniterScala()) shouldBe b.compactJsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.compactJsonString
      toString(b.playJson()) shouldBe b.compactJsonString
      toString(b.playJsonJsoniter()) shouldBe b.compactJsonString
      toString(b.sprayJson()) shouldBe b.compactJsonString
      toString(b.uPickle()) shouldBe b.compactJsonString
      toString(b.weePickle()) shouldBe b.compactJsonString
    }
  }
}