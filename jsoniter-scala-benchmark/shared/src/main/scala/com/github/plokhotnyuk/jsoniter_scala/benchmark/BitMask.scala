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

object BitMask {
  def toBitMask(vs: Array[Int], mvl: Int): Array[Long] = {
    val vsl = vs.length
    if (vsl == 0) new Array[Long](0)
    else {
      var mv = -1
      var i = 0
      while (i < vsl) {
        val v = vs(i)
        if (v >= mvl) throw new IllegalArgumentException(s"the bit set value: $v exceeds the limit: $mvl")
        if (v > mv) mv = v
        i += 1
      }
      val bs = new Array[Long]((mv >>> 6) + 1)
      i = 0
      while (i < vsl) {
        val v = vs(i)
        bs(v >>> 6) |= 1L << v
        i += 1
      }
      bs
    }
  }
}