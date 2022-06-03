package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GitHubActionsAPIReadingSpec extends BenchmarkSpecBase {
  def benchmark = new GitHubActionsAPIReading

  "GitHubActionsAPIReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJawn() shouldBe benchmark.obj
      benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.ninnyJson() shouldBe benchmark.obj
      benchmark.smithy4s() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
      benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(0) = 'x'.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJawn())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.ninnyJson())
      intercept[Throwable](b.smithy4s())
      intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.weePickle())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.zioJson())
    }
  }
}