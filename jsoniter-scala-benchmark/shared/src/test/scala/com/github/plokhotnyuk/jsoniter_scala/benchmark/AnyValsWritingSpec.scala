package com.github.plokhotnyuk.jsoniter_scala.benchmark

class AnyValsWritingSpec extends BenchmarkSpecBase {
  def benchmark = new AnyValsWriting
  
  "AnyValsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString1
      toString(b.borer()) shouldBe b.jsonString1
      toString(b.circe()) shouldBe b.jsonString1
      //FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for com.github.plokhotnyuk.jsoniter_scala.b.AnyVals
      //toString(b.dslJsonScala()) shouldBe b.jsonString1
      toString(b.jacksonScala()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString1
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.playJson()) shouldBe b.jsonString3
      toString(b.sprayJson()) shouldBe b.jsonString2
      toString(b.uPickle()) shouldBe b.jsonString1
      toString(b.weePickle()) shouldBe b.jsonString1
    }
  }
}