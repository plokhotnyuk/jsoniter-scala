package com.github.plokhotnyuk.jsoniter_scala.benchmark

class AnyValsWritingSpec extends BenchmarkSpecBase {
  def benchmark = new AnyValsWriting

  "AnyValsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString1
      toString(b.borer()) shouldBe b.jsonString1
      toString(b.circe()) shouldBe b.jsonString1
      toString(b.circeJsoniter()) shouldBe b.jsonString1
      //FIXME: DSL-JSON wraps AnyVal values
      //toString(b.dslJsonScala()) shouldBe b.jsonString1
      toString(b.jacksonScala()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString1
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.playJson()) shouldBe b.jsonString3
      toString(b.playJsonJsoniter()) shouldBe b.jsonString1
      toString(b.smithy4s()) shouldBe b.jsonString1
      toString(b.sprayJson()) shouldBe b.jsonString2
      toString(b.uPickle()) shouldBe b.jsonString1
      toString(b.weePickle()) shouldBe b.jsonString1
      toString(b.zioJson()) shouldBe b.jsonString1
    }
  }
}