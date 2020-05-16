package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfBytesWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBytesWriting {
    setup()
  }
  
  "ArrayOfBytesWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.borer()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      //FIXME: DSL-JSON serializes a byte array to the base64 string
      //toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString
      toString(benchmark.weePickle()) shouldBe benchmark.jsonString
    }
  }
}