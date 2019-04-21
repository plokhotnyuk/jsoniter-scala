package com.github.plokhotnyuk.jsoniter_scala.benchmark

class AnyRefsWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new AnyRefsWriting
  
  "AnyRefsWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString1
      toString(benchmark.circe()) shouldBe benchmark.jsonString1
      toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString1
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString1
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString1
      toString(benchmark.playJson()) shouldBe benchmark.jsonString1
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString2
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString1
    }
  }
}