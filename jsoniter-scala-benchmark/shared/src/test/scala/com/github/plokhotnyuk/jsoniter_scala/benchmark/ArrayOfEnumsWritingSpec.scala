package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfEnumsWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfEnumsWriting = new ArrayOfEnumsWriting {
    setup()
  }
  
  "ArrayOfEnumsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.borer()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      toString(b.playJsonJsoniter()) shouldBe b.jsonString
      toString(b.sprayJson()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString
      toString(b.zioJson()) shouldBe b.jsonString
    }
  }
}