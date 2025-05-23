package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class BigDecimalReadingSpec extends BenchmarkSpecBase {
  def benchmark: BigDecimalReading = new BigDecimalReading {
    setup()
  }

  "BigDecimalReading" should {
    "read properly" in {
      // FIXME: borer parses up to 200 digits only
      // benchmark.borer() shouldBe benchmark.sourceObj
      benchmark.circe() shouldBe benchmark.sourceObj
      // FIXME: circe-jsoniter parses up to 308 digits only
      // benchmark.circeJsoniter() shouldBe benchmark.sourceObj
      benchmark.jacksonScala() shouldBe benchmark.sourceObj
      benchmark.json4sJackson() shouldBe benchmark.obj
      // FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
      // benchmark.json4sNative() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.sourceObj
      // FIXME: Play-JSON: don't know how to tune precision for parsing of BigDecimal values
      // benchmark.playJson() shouldBe benchmark.sourceObj
      // FIXME: smithy4sJson parses up to 308 digits only
      // benchmark.smithy4sJson() shouldBe benchmark.sourceObj
      benchmark.sprayJson() shouldBe benchmark.sourceObj
      benchmark.uPickle() shouldBe benchmark.sourceObj
      benchmark.weePickle() shouldBe benchmark.sourceObj
      benchmark.zioJson() shouldBe benchmark.sourceObj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "{}".getBytes(UTF_8)
      // FIXME: borer parses up to 200 digits only
      // intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      // FIXME: circe-jsoniter parses up to 308 digits only
      // intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.json4sJackson())
      // FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
      // intercept[Throwable](b.json4sNative())
      intercept[Throwable](b.jsoniterScala())
      // FIXME: smithy4sJson parses up to 308 digits only
      // intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.weePickle())
      intercept[Throwable](b.zioJson())
    }
  }
}