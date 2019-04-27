package com.github.plokhotnyuk.jsoniter_scala.benchmark

class StringOfEscapedCharsWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new StringOfEscapedCharsWriting {
    setup()
  }

  "StringOfEscapedCharsWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      //FIXME: DSL-JSON doesn't support escaping of non-ASCII characters
      //toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString2
      toString(benchmark.jsoniterJava()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString2
      //FIXME: Spray-JSON doesn't support escaping of non-ASCII characters
      //toString(benchmark.sprayJson()) shouldBe benchmark.jsonString2
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}