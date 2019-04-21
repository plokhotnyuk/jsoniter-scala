package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableBitSetWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new MutableBitSetWriting {
    setup()
  }
  
  "MutableBitSetWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Circe doesn't support writing of bitsets
      //toString(benchmark.circe()) shouldBe benchmark.jsonString
      toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle doesn't support mutable bitsets
      //toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}