package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfInstantsWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfInstantsWriting {
    setup()
  }
  
  "ArrayOfInstantsWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.borerCbor()) shouldBe toString(benchmark.cborBytes)
      toString(benchmark.borerJson()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}