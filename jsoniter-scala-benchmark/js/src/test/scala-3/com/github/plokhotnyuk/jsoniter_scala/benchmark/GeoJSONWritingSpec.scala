package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GeoJSONWritingSpec extends BenchmarkSpecBase {
  def benchmark: GeoJSONWriting = new GeoJSONWriting {
    setup()
  }

  "GeoJSONWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString1
      toString(b.circe()) shouldBe b.jsonString1
      toString(b.circeJsoniter()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString1
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.smithy4sJson()) shouldBe b.jsonString1
      toString(b.uPickle()) shouldBe b.jsonString1
      //FIXME: zio-json codec doesn't compile
      //toString(b.zioJson()) shouldBe b.jsonString1
    }
  }
}