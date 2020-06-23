package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GitHubActionsAPIWritingSpec extends BenchmarkSpecBase {
  val benchmark = new GitHubActionsAPIWriting
  
  "GoogleMapsAPIWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.compactJsonString1
      toString(benchmark.borer()) shouldBe benchmark.compactJsonString1
      toString(benchmark.circe()) shouldBe benchmark.compactJsonString1
      toString(benchmark.jacksonScala()) shouldBe benchmark.compactJsonString1
      toString(benchmark.jsoniterScala()) shouldBe benchmark.compactJsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.compactJsonString1
      toString(benchmark.sprayJson()) shouldBe benchmark.compactJsonString2
      toString(benchmark.weePickle()) shouldBe benchmark.compactJsonString1
    }
  }
}