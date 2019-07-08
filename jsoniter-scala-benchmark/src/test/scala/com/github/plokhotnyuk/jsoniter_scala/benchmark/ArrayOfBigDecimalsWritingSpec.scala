package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfBigDecimalsWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBigDecimalsWriting {
    setup()
  }
  
  "ArrayOfBigDecimalsWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()).toUpperCase shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      toString(benchmark.scalikeJackson()) shouldBe benchmark.jsonString
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}