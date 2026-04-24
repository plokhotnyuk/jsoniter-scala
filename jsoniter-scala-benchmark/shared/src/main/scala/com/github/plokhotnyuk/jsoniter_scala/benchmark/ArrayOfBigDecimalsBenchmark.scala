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

import java.math.MathContext
import java.nio.charset.StandardCharsets.UTF_8
import org.openjdk.jmh.annotations.{Param, Setup}

abstract class ArrayOfBigDecimalsBenchmark extends CommonParams {
  var sourceObj: Array[BigDecimal] = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString: String = _
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 512

  @Setup
  def setup(): Unit = {
    sourceObj = (1 to size).map { i =>
      BigDecimal(BigInt(Array.fill((i % 14) + 1)((i | 0x1).toByte)), i % 38).round(MathContext.DECIMAL128)
    }.toArray // up to 112-bit BigInt numbers rounded to IEEE 754 Decimal128 format (34 decimal digits)
    jsonString = sourceObj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 128/*to avoid possible out-of-bounds error*/)
  }

  private[benchmark] def obj: Array[BigDecimal] = {
    val xs = sourceObj
    val l = xs.length
    val ys = new Array[BigDecimal](l)
    var i = 0
    while (i < l) {
      val x = xs(i) // to avoid internal caching of the string representation
      val bd = x.bigDecimal
      ys(i) = new BigDecimal(new java.math.BigDecimal(bd.unscaledValue, bd.scale), x.mc)
      i += 1
    }
    ys
  }
}