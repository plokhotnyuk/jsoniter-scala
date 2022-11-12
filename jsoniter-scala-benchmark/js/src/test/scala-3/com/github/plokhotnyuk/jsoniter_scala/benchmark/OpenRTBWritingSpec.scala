package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBWritingSpec extends BenchmarkSpecBase {
  def benchmark: OpenRTBWriting = new OpenRTBWriting {
    setup()
  }

  "OpenRTBWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      //FIXME: Circe require a custom codec
      //toString(b.circe()) shouldBe b.jsonString
      //toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
    }
  }
}