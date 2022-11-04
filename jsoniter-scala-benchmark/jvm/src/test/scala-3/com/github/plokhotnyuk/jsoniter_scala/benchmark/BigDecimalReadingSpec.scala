package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class BigDecimalReadingSpec extends BenchmarkSpecBase {
  def benchmark: BigDecimalReading = new BigDecimalReading {
    setup()
  }

  "BigDecimalReading" should {
    "read properly" in {
      benchmark.borer() shouldBe benchmark.sourceObj
      benchmark.jacksonScala() shouldBe benchmark.sourceObj
      benchmark.json4sJackson() shouldBe benchmark.obj
      //FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
      //benchmark.json4sNative() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.sourceObj
      //FIXME: smithy4sJson: don't know how to tune precision for parsing of BigDecimal values
      //benchmark.smithy4sJson() shouldBe benchmark.sourceObj
      benchmark.weePickle() shouldBe benchmark.sourceObj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "{}".getBytes(UTF_8)
      intercept[Throwable](b.borer())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.json4sJackson())
      //FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
      //intercept[Throwable](b.json4sNative())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.weePickle())
    }
  }
}