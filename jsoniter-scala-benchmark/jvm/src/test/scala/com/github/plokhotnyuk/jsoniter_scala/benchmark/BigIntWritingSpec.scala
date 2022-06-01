package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BigIntWritingSpec extends BenchmarkSpecBase {
  def benchmark: BigIntWriting = new BigIntWriting {
    setup()
  }

  "BigIntWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.borer()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.dslJsonScala()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      //FIXME: Play-JSON serializes BigInt values as floating point numbers with a scientific representation
      //toString(b.playJson()) shouldBe b.jsonString
      toString(b.smithy4s()) shouldBe b.jsonString
      toString(b.sprayJson()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
      //FIXME: weePickle serializes BigInt values as JSON strings
      //toString(b.weePickle()) shouldBe b.jsonString
      toString(b.zioJson()) shouldBe b.jsonString
    }
  }
}