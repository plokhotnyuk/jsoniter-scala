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

object HashCodeCollider {
  def zeroHashCodeStrings: Iterator[String] = {
    def charAndHash(h: Int): Iterator[(Char, Int)] = ('!' to '~').iterator.map(ch => (ch, (h + ch) * 31))

    for {
      (ch0, h0) <- charAndHash(0)
      (ch1, h1) <- charAndHash(h0)
      (ch2, h2) <- charAndHash(h1) if ((h2 + 32) * 923521 ^ (h2 + 127) * 923521) < 0
      (ch3, h3) <- charAndHash(h2) if ((h3 + 32) * 29791 ^ (h3 + 127) * 29791) < 0
      (ch4, h4) <- charAndHash(h3) if ((h4 + 32) * 961 ^ (h4 + 127) * 961) < 0
      (ch5, h5) <- charAndHash(h4) if ((h5 + 32) * 31 ^ (h5 + 127) * 31) < 0
      (ch6, h6) <- charAndHash(h5) if (h6 + 32 ^ h6 + 127) < 0
      (ch7, _) <- charAndHash(h6) if h6 + ch7 == 0
    } yield new String(Array(ch0, ch1, ch2, ch3, ch4, ch5, ch6, ch7))
  }
}