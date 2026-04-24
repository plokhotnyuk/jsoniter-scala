/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class BigIntReadingSpec extends BenchmarkSpecBase {
  def benchmark: BigIntReading = new BigIntReading {
    setup()
  }

  "BigIntReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      // FIXME: borer parses up to 200 digits only
      // benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      // FIXME: circe-jsoniter parses up to 308 digits only
      // benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.dslJsonScala() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.json4sJackson() shouldBe benchmark.obj
      // FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
      // benchmark.json4sNative() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      // FIXME: Play-JSON looses significant digits in BigInt values
      // benchmark.playJson() shouldBe benchmark.obj
      // FIXME: smithy4sJson parses up to 308 digits only
      // benchmark.smithy4sJson() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
      benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "{}".getBytes(UTF_8)
      intercept[Throwable](b.avSystemGenCodec())
      // FIXME: borer parses up to 200 digits only
      // intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      // FIXME: circe-jsoniter parses up to 308 digits only
      // intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.dslJsonScala())
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