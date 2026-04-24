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

class BitMaskSpec extends BenchmarkSpecBase {
  "BitMask" should {
    "build properly" in {
      BitMask.toBitMask(Array[Int](), 4).toList shouldBe List()
      BitMask.toBitMask(Array(1, 2, 3), 4).toList shouldBe List(14)
      BitMask.toBitMask(Array(3, 2, 1), 4).toList shouldBe List(14)
    }
    "fail on invalid input" in {
      intercept[Throwable](BitMask.toBitMask(Array(3, 2, 1), 1))
      intercept[Throwable](BitMask.toBitMask(Array(3, 2, 1), 2))
      intercept[Throwable](BitMask.toBitMask(Array(3, 2, 1), 3))
    }
  }
}