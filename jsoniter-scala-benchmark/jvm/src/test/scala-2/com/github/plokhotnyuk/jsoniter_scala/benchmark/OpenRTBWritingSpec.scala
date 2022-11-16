package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBWritingSpec extends BenchmarkSpecBase {
  def benchmark: OpenRTBWriting = new OpenRTBWriting {
    setup()
  }

  "OpenRTBWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.borer()) shouldBe b.jsonString
      //FIXME: Circe require a custom codec
      //toString(b.circe()) shouldBe b.jsonString
      //toString(b.circeJsoniter()) shouldBe b.jsonString
      //FIXME: Jackson serializes fields with default values
      //toString(b.jacksonScala()) shouldBe b.jsonString
      //FIXME: json4s.jackson serializes fields with default values
      //toString(b.json4sJackson()) shouldBe b.jsonString
      //FIXME: json4s.native serializes fields with default values
      //toString(b.json4sNative()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      toString(b.playJsonJsoniter()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
      //FIXME: Spray-JSON serializes fields with default values
      //toString(b.sprayJson()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString
      //FIXME: Zio-JSON serializes empty collections
      //toString(b.zioJson()) shouldBe b.jsonString
    }
  }
}