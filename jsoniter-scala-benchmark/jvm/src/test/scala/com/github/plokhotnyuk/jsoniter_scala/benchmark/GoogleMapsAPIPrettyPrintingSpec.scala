package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIPrettyPrintingSpec extends BenchmarkSpecBase {
  def benchmark = new GoogleMapsAPIPrettyPrinting

  "GoogleMapsAPIPrettyPrinting" should {
    "pretty print properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString2
      toString(b.circe()) shouldBe b.jsonString1
      toString(b.circeJsoniter()) shouldBe b.jsonString2
      toString(b.jacksonScala()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString2
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString2
      toString(b.playJson()) shouldBe b.jsonString1
      toString(b.playJsonJsoniter()) shouldBe b.jsonString2
      toString(b.sprayJson()) shouldBe b.jsonString2
      toString(b.uPickle()) shouldBe b.jsonString2
      toString(b.weePickle()) shouldBe b.jsonString2
      toString(b.zioJson()) shouldBe b.jsonString1
    }
  }
}