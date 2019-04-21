package com.github.plokhotnyuk.jsoniter_scala.benchmark

class TwitterAPIWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new TwitterAPIWriting
  
  "TwitterAPIWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe TwitterAPI.compactJsonString
      //FIXME: circe serializes empty collections
      //toString(benchmark.circe()) shouldBe TwitterAPI.compactJsonString
      //FIXME: DSL-JSON serializes empty collections
      //toString(benchmark.dslJsonScala()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.jacksonScala()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.jsoniterScala()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe TwitterAPI.compactJsonString
      //FIXME: Play-JSON serializes empty collections
      //toString(benchmark.playJson()) shouldBe TwitterAPI.compactJsonString
      //FIXME: Spray-JSON serializes empty collections
      //toString(benchmark.sprayJson()) shouldBe TwitterAPI.compactJsonString
      //FIXME: uPickle serializes empty collections
      //toString(benchmark.uPickle()) shouldBe TwitterAPI.compactJsonString
    }
  }
}