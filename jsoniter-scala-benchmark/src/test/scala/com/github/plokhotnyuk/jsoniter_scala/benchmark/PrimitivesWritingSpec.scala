package com.github.plokhotnyuk.jsoniter_scala.benchmark

class PrimitivesWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new PrimitivesWriting
  
  "PrimitivesWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString1
      toString(benchmark.borerJson()) shouldBe benchmark.jsonString1
      toString(benchmark.circe()) shouldBe benchmark.jsonString1
      //FIXME: DSL-JSON cannot create decoder for com.github.plokhotnyuk.jsoniter_scala.benchmark.Primitives
      //toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString1
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString1
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString1
      toString(benchmark.playJson()) shouldBe benchmark.jsonString3
      toString(benchmark.scalikeJackson()) shouldBe benchmark.jsonString1
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString2
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString1
    }
  }
}