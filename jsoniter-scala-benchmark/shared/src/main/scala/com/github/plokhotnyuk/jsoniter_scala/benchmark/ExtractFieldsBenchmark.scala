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

import org.openjdk.jmh.annotations.{Param, Setup}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.HashCodeCollider._
import com.github.plokhotnyuk.jsoniter_scala.core._
import java.nio.charset.StandardCharsets.UTF_8

abstract class ExtractFieldsBenchmark extends CommonParams {
  var jsonBytes: Array[Byte] = _
  var obj: ExtractFields = ExtractFields("s", 1)
  var jsonString: String = _
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 512

  @Setup
  def setup(): Unit = {
    val value = """{"number":0.0,"boolean":false,"string":null}"""
    jsonString = zeroHashCodeStrings.map(s => writeToString(s)(JsoniterScalaCodecs.stringCodec)).take(size)
      .mkString("""{"s":"s",""", s""":$value,""", s""":$value,"i":1}""") // test for hashcode collision vulnerability
    //jsonString = s"""{"s":"s","n":${"7" * size},"i":1}""" // test for big number parsing vulnerability
    //jsonString = s"""{"s":"s","n":"${"x" * size}","i":1}""" // test for big string parsing vulnerability
    //jsonString = s"""{"s":"s","n":${"[" * size + "]" * size},"i":1}""" // test for nested JSON array vulnerability
    //jsonString = s"""{"s":"s",${""""n":{""" * size + "}" * size},"i":1}""" // test for nested JSON object vulnerability
    jsonBytes = jsonString.getBytes(UTF_8)
  }
}