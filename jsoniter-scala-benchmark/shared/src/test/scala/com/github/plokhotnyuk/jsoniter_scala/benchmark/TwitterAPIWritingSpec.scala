package com.github.plokhotnyuk.jsoniter_scala.benchmark

class TwitterAPIWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new TwitterAPIWriting
  
  "TwitterAPIWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.compactJsonString
      toString(benchmark.borer()) shouldBe benchmark.compactJsonString
      //FIXME: Circe serializes empty collections
      //toString(benchmark.circe()) shouldBe benchmark.compactJsonString
      //FIXME: DSL-JSON serializes empty collections
      //toString(benchmark.dslJsonScala()) shouldBe benchmark.compactJsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.compactJsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.compactJsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.compactJsonString
      //FIXME: Play-JSON serializes empty collections
      //toString(benchmark.playJson()) shouldBe benchmark.compactJsonString
      //FIXME: Spray-JSON serializes empty options and collections
      //toString(benchmark.sprayJson()) shouldBe benchmark.compactJsonString
      toString(benchmark.uPickle()) shouldBe benchmark.compactJsonString
      toString(benchmark.weePickle()) shouldBe benchmark.compactJsonString
    }
  }
}