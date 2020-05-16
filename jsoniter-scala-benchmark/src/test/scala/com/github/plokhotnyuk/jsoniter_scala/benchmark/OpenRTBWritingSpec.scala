package com.github.plokhotnyuk.jsoniter_scala.benchmark

class OpenRTBWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new OpenRTBWriting

  "OpenRTBWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.borer()) shouldBe benchmark.jsonString
      //FIXME: Circe serializes fields with default values
      //toString(benchmark.circe()) shouldBe benchmark.jsonString
      //FIXME: Jackson serializes fields with default values
      //toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      //FIXME: Play-JSON serializes lists with default values
      //toString(benchmark.playJson()) shouldBe benchmark.jsonString
      //FIXME: Spray-JSON serializes fields with default values
      //toString(benchmark.sprayJson()) shouldBe benchmark.jsonString
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString
      toString(benchmark.weePickle()) shouldBe benchmark.jsonString
    }
  }
}