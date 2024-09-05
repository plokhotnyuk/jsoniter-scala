package com.github.plokhotnyuk.jsoniter_scala.benchmark

class IntMapOfBooleansWritingSpec extends BenchmarkSpecBase {
  def benchmark: IntMapOfBooleansWriting = new IntMapOfBooleansWriting {
    setup()
  }

  "IntMapOfBooleansWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      //FIXME: DSL-JSON throws java.lang.ClassCastException: scala.Tuple2 cannot be cast to java.lang.Boolean
      //toString(b.dslJsonScala()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.json4sJackson()) shouldBe b.jsonString
      toString(b.json4sNative()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      toString(b.playJsonJsoniter()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
      //FIXME: weePickle throws java.lang.ClassCastException: class scala.Tuple2 cannot be cast to class java.lang.Boolean
      //toString(b.weePickle()) shouldBe b.jsonString
    }
  }
}