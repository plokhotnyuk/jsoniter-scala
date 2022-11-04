package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBWritingSpec extends BenchmarkSpecBase {
  def benchmark: OpenRTBWriting = new OpenRTBWriting {
    setup()
  }

  "OpenRTBWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      //FIXME: Jackson serializes fields with default values
      //toString(b.jacksonScala()) shouldBe b.jsonString
      //FIXME: json4s.jackson serializes fields with default values
      //toString(b.json4sJackson()) shouldBe b.jsonString
      //FIXME: json4s.native serializes fields with default values
      //toString(b.json4sNative()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString
    }
  }
}