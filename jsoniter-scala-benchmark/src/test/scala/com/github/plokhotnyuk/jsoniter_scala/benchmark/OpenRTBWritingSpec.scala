package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new OpenRTBWriting

  "OpenRTBWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe OpenRTB.jsonString
      toString(benchmark.borerJson()) shouldBe OpenRTB.jsonString
      //FIXME: Circe serializes fields with default values
      //toString(benchmark.circe()) shouldBe OpenRTB.jsonString
      //FIXME: Jackson serializes fields with default values
      //toString(benchmark.jacksonScala()) shouldBe OpenRTB.jsonString
      toString(benchmark.jsoniterScala()) shouldBe OpenRTB.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe OpenRTB.jsonString
      //FIXME: Spray-JSON serializes fields with default values
      //toString(benchmark.sprayJson()) shouldBe OpenRTB.jsonString
      toString(benchmark.uPickle()) shouldBe OpenRTB.jsonString
    }
  }
}