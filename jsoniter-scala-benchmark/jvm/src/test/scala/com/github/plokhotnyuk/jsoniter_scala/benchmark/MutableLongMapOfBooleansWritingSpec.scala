package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableLongMapOfBooleansWritingSpec extends BenchmarkSpecBase {
  def benchmark: MutableLongMapOfBooleansWriting = new MutableLongMapOfBooleansWriting {
    setup()
  }

  "MutableLongMapOfBooleansWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      //FIXME: DSL-JSON doesn't support mutable.LongMap
      //toString(b.dslJsonScala()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      toString(b.playJsonJsoniter()) shouldBe b.jsonString
    }
  }
}