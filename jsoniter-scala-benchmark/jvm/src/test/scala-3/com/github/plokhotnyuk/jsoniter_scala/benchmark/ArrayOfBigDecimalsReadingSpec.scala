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

class ArrayOfBigDecimalsReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfBigDecimalsReading = new ArrayOfBigDecimalsReading {
    setup()
  }

  "ArrayOfBigDecimalsReading" should {
    "read properly" in {
      benchmark.borer() shouldBe benchmark.sourceObj
      benchmark.circe() shouldBe benchmark.sourceObj
      benchmark.circeJsoniter() shouldBe benchmark.sourceObj
      benchmark.jacksonScala() shouldBe benchmark.sourceObj
      benchmark.json4sJackson() shouldBe benchmark.sourceObj
      benchmark.json4sNative() shouldBe benchmark.sourceObj
      benchmark.jsoniterScala() shouldBe benchmark.sourceObj
      benchmark.playJson() shouldBe benchmark.sourceObj
      benchmark.playJsonJsoniter() shouldBe benchmark.sourceObj
      benchmark.smithy4sJson() shouldBe benchmark.sourceObj
      benchmark.sprayJson() shouldBe benchmark.sourceObj
      benchmark.uPickle() shouldBe benchmark.sourceObj
      benchmark.weePickle() shouldBe benchmark.sourceObj
      benchmark.zioBlocks() shouldBe benchmark.sourceObj
      benchmark.zioJson() shouldBe benchmark.sourceObj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes = "[true]".getBytes(UTF_8)
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.json4sJackson())
      intercept[Throwable](b.json4sNative())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.weePickle())
      intercept[Throwable](b.zioBlocks())
      intercept[Throwable](b.zioJson())
    }
  }
}