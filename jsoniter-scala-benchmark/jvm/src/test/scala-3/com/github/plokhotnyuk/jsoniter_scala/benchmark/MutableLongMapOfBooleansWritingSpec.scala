package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableLongMapOfBooleansWritingSpec extends BenchmarkSpecBase {
  def benchmark: MutableLongMapOfBooleansWriting = new MutableLongMapOfBooleansWriting {
    setup()
  }

  "MutableLongMapOfBooleansWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.json4sJackson()) shouldBe b.jsonString
      toString(b.json4sNative()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      //FIXME: weePickle throws java.lang.ClassCastException: class scala.Tuple2 cannot be cast to class java.lang.Boolean
      //toString(b.weePickle()) shouldBe b.jsonString
    }
  }
}