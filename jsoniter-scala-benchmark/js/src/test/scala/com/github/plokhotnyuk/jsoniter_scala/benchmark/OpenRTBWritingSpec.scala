package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBWritingSpec extends BenchmarkSpecBase {
  def benchmark = new OpenRTBWriting

  "OpenRTBWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.borer()) shouldBe b.jsonString
      //FIXME: Circe serializes fields with default values
      //toString(b.circe()) shouldBe b.jsonString
      //toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      //FIXME: Play-JSON serializes lists with default values
      //toString(b.playJson()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
      //FIXME: Zio-JSON serializes empty collections
      //toString(b.zioJson()) shouldBe b.jsonString
    }
  }
}