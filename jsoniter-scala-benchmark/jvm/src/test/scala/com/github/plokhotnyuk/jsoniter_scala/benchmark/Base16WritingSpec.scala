package com.github.plokhotnyuk.jsoniter_scala.benchmark

class Base16WritingSpec extends BenchmarkSpecBase {
  def benchmark: Base16Writing = new Base16Writing {
    setup()
  }
  
  "Base64Writing" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.borer()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
    }
  }
}