package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GitHubActionsAPIWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new GitHubActionsAPIWriting
  
  "GoogleMapsAPIWriting" should {
    "write properly" in {
      toString(benchmark.borerJson()) shouldBe GitHubActionsAPI.compactJsonString1
      toString(benchmark.jacksonScala()) shouldBe GitHubActionsAPI.compactJsonString1
      toString(benchmark.jsoniterScala()) shouldBe GitHubActionsAPI.compactJsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe GitHubActionsAPI.compactJsonString1
      toString(benchmark.sprayJson()) shouldBe GitHubActionsAPI.compactJsonString2
      toString(benchmark.weePickle()) shouldBe GitHubActionsAPI.compactJsonString1
    }
  }
}