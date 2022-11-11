package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class BigDecimalReadingSpec extends BenchmarkSpecBase {
  def benchmark: BigDecimalReading = new BigDecimalReading {
    setup()
  }

  "BigDecimalReading" should {
    "read properly" in {
      benchmark.borer() shouldBe benchmark.sourceObj
      benchmark.circe() shouldBe benchmark.sourceObj
      benchmark.circeJsoniter() shouldBe benchmark.sourceObj
      benchmark.jsoniterScala() shouldBe benchmark.sourceObj
      //FIXME: smithy4sJson: don't know how to tune precision for parsing of BigDecimal values
      //benchmark.smithy4sJson() shouldBe benchmark.sourceObj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "{}".getBytes(UTF_8)
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jsoniterScala())
    }
  }
}