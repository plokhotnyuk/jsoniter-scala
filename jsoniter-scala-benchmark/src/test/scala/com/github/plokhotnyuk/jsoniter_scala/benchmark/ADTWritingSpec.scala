package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ADTWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new ADTWriting
  
  "ADTWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString1
      toString(benchmark.borerJson()) shouldBe benchmark.jsonString1
      toString(benchmark.circe()) shouldBe benchmark.jsonString2
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString1
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString1
      toString(benchmark.playJson()) shouldBe benchmark.jsonString1
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString2
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString1
      toString(benchmark.weePickle()) shouldBe benchmark.jsonString1
    }
  }
}