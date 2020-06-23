package com.github.plokhotnyuk.jsoniter_scala.benchmark

class Base16WritingSpec extends BenchmarkSpecBase {
  val benchmark = new Base16Writing {
    setup()
  }
  
  "Base64Writing" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.borer()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
    }
  }
}