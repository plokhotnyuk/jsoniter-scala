package com.github.plokhotnyuk.jsoniter_scala.benchmark

class MutableLongMapOfBooleansWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new MutableLongMapOfBooleansWriting {
    setup()
  }
  
  "MutableLongMapOfBooleansWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      //FIXME: DSL-JSON doesn't support mutable.LongMap
      //toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle doesn't support mutable.LongMap
      //toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}