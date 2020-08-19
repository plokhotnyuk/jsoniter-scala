package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BigDecimalWritingSpec extends BenchmarkSpecBase {
  def benchmark: BigDecimalWriting = new BigDecimalWriting {
    setup()
  }
  
  "BigDecimalWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.borer()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.dslJsonScala()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      BigDecimal(toString(b.playJson())) shouldBe b.obj
      toString(b.sprayJson()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
      //FIXME: weePickle serializes BigDecimal values as JSON strings
      //toString(b.weePickle()) shouldBe b.jsonString
    }
  }
}