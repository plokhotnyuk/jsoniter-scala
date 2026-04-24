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

class ArrayOfFloatsWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfFloatsWriting = new ArrayOfFloatsWriting {
    setup()
  }

  "ArrayOfFloatsWriting" should {
    "write properly" in {
      val b = benchmark
      check(toString(b.avSystemGenCodec()), b.jsonString)
      check(toString(b.borer()), b.jsonString)
      check(toString(b.circe()), b.jsonString)
      check(toString(b.circeJsoniter()), b.jsonString)
      check(toString(b.dslJsonScala()), b.jsonString)
      check(toString(b.jacksonScala()), b.jsonString)
      check(toString(b.json4sJackson()), b.jsonString)
      check(toString(b.json4sNative()), b.jsonString)
      check(toString(b.jsoniterScala()), b.jsonString)
      check(toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()), b.jsonString)
      check(toString(b.playJson()), b.jsonString)
      check(toString(b.playJsonJsoniter()), b.jsonString)
      check(toString(b.smithy4sJson()), b.jsonString)
      check(toString(b.sprayJson()), b.jsonString)
      check(toString(b.uPickle()), b.jsonString)
      check(toString(b.weePickle()), b.jsonString)
      check(toString(b.zioBlocks()), b.jsonString)
      check(toString(b.zioJson()), b.jsonString)
      check(toString(b.zioSchemaJson()), b.jsonString)
    }
  }

  private[this] def check(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
      require(a.toFloat == e.toFloat, s"expected: $e, but got: $a when parsed back to float")
    }
}