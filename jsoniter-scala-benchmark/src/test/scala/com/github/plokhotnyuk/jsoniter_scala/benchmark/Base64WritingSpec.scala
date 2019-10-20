package com.github.plokhotnyuk.jsoniter_scala.benchmark

class Base64WritingSpec extends BenchmarkSpecBase {
  private val benchmark = new Base64Writing {
    setup()
  }
  
  "Base64Writing" should {
    "write properly" in {
      toString(benchmark.borerJson()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
    }
  }
}