package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new OpenRTBWriting

  "OpenRTBWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe OpenRTB.jsonString
      //FIXME: Borer serializes fields with default values
      //toString(benchmark.borerJson()) shouldBe OpenRTB.jsonString
      toString(benchmark.jsoniterScala()) shouldBe OpenRTB.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe OpenRTB.jsonString
    }
  }
}