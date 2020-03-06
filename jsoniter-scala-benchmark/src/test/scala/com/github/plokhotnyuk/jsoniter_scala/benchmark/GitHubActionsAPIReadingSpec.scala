package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GitHubActionsAPIReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new GitHubActionsAPIReading
  
  "GitHubActionsAPIReading" should {
    "read properly" in {
      benchmark.borerJson() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
    }
  }
}