package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GitHubActionsAPIWritingSpec extends BenchmarkSpecBase {
  def benchmark = new GitHubActionsAPIWriting
  
  "GoogleMapsAPIWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.compactJsonString1
      toString(b.borer()) shouldBe b.compactJsonString1
      toString(b.circe()) shouldBe b.compactJsonString1
      toString(b.jacksonScala()) shouldBe b.compactJsonString1
      toString(b.jsoniterScala()) shouldBe b.compactJsonString1
      toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()) shouldBe b.compactJsonString1
      toString(b.sprayJson()) shouldBe b.compactJsonString2
      toString(b.weePickle()) shouldBe b.compactJsonString1
      toString(b.zioJson()) shouldBe b.compactJsonString1
    }
  }
}