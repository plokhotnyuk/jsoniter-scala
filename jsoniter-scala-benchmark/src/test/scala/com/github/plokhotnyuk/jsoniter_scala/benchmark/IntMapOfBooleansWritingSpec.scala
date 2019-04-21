package com.github.plokhotnyuk.jsoniter_scala.benchmark

class IntMapOfBooleansWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new IntMapOfBooleansWriting {
    setup()
  }
  
  "IntMapOfBooleansWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Circe doesn't support IntMap
      //toString(benchmark.circe()) shouldBe benchmark.jsonString
      //FIXME: DSL-JSON throws java.lang.ClassCastException: scala.Tuple2 cannot be cast to java.lang.Boolean
      //toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle doesn't support IntMap
      //toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}