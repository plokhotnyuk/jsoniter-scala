package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfLocalTimesWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfLocalTimesWriting {
    setup()
  }
  
  "ArrayOfLocalTimesWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      //FIXME DSL-JSON sometime throws: java.lang.ArrayIndexOutOfBoundsException: Index 13638 out of bounds for length 13638
      //toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}