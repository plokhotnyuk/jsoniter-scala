package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GitHubActionsAPIWritingSpec extends BenchmarkSpecBase {
  def benchmark: GitHubActionsAPIWriting = new GitHubActionsAPIWriting {
    setup()
  }

  "GoogleMapsAPIWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.compactJsonString1
      toString(b.circe()) shouldBe b.compactJsonString1
      toString(b.circeJsoniter()) shouldBe b.compactJsonString1
      toString(b.jacksonScala()) shouldBe b.compactJsonString1
      toString(b.json4sJackson()) shouldBe b.compactJsonString1
      toString(b.json4sNative()) shouldBe b.compactJsonString1
      toString(b.jsoniterScala()) shouldBe b.compactJsonString1
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.compactJsonString1
      toString(b.smithy4sJson()) shouldBe b.compactJsonString1
      toString(b.uPickle()) shouldBe b.compactJsonString1
      toString(b.weePickle()) shouldBe b.compactJsonString1
      toString(b.zioJson()) shouldBe b.compactJsonString1
    }
  }
}