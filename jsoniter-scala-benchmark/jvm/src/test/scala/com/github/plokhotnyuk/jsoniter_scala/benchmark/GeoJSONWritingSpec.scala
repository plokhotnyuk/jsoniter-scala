package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GeoJSONWritingSpec extends BenchmarkSpecBase {
  def benchmark = new GeoJSONWriting

  "GeoJSONWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString1
      toString(b.borer()) shouldBe b.jsonString1
      toString(b.circe()) shouldBe b.jsonString2
      toString(b.circeJsoniter()) shouldBe b.jsonString2
      toString(b.jacksonScala()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString1
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.playJson()) shouldBe b.jsonString1
      toString(b.playJsonJsoniter()) shouldBe b.jsonString1
      toString(b.smithy4s()) shouldBe b.jsonString1
      toString(b.sprayJson()) shouldBe b.jsonString3
      toString(b.uPickle()) shouldBe b.jsonString1
      toString(b.weePickle()) shouldBe b.jsonString1
      toString(b.zioJson()) shouldBe b.jsonString1
    }
  }
}