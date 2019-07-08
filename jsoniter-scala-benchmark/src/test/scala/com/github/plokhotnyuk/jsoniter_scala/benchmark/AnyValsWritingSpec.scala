package com.github.plokhotnyuk.jsoniter_scala.benchmark

class AnyValsWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new AnyValsWriting
  
  "AnyValsWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString1
      toString(benchmark.circe()) shouldBe benchmark.jsonString1
      //FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for com.github.plokhotnyuk.jsoniter_scala.benchmark.AnyVals
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