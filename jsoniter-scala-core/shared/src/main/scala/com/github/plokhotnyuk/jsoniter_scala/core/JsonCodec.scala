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

package com.github.plokhotnyuk.jsoniter_scala.core

import scala.{specialized => sp}

/**
  * A codec for both JSON values and JSON keys of type `A`.
  *
  * BEWARE: `JsonCodec[A]` can hit a JDK scalability bottleneck of type checks for classes with multiple interfaces,
  * so use it sparingly. For details see:
  *
  *   - [[https://www.youtube.com/watch?v=PxcO3WHqmng]]
  *   - [[https://bugs.openjdk.org/browse/JDK-8180450]]
  *   - [[https://github.com/netty/netty/search?q=JDK-8180450&type=issues]]
  *   - [[https://redhatperf.github.io/post/type-check-scalability-issue]]
  *   - [[https://netflixtechblog.com/seeing-through-hardware-counters-a-journey-to-threefold-performance-increase-2721924a2822]]
  */
trait JsonCodec[@sp A] extends JsonValueCodec[A] with JsonKeyCodec[A]

/**
  * A codec that decodes and encodes JSON values of type `A`.
  */
trait JsonValueCodec[@sp A] extends Serializable {
  /**
    * Decodes a JSON value of type `A`.
    *
    * @param in the reader of JSON input
    * @param default the value to return for a JSON `null`, and to initialize local variables
    * @throws JsonReaderException if the JSON value doesn't match type `A`
    */
  def decodeValue(in: JsonReader, default: A): A

  /**
    * Encodes `x` as a JSON value.
    *
    * @param x the value to encode
    * @param out the writer of JSON output
    * @throws JsonWriterException if `x` can't be encoded as RFC-8259 compliant JSON
    */
  def encodeValue(x: A, out: JsonWriter): Unit

  /**
    * Returns the placeholder value that generated codecs use to initialize local variables of fields that have an
    * injected implicit codec.
    *
    * See the `jsoniter-scala-macros` sources and tests for usages of `.nullValue` calls.
    */
  def nullValue: A
}

/**
  * A codec that decodes and encodes JSON keys of type `A`.
  */
trait JsonKeyCodec[@sp A] extends Serializable {
  /**
    * Decodes a JSON key of type `A`.
    *
    * @param in the reader of JSON input
    * @throws JsonReaderException if the input isn't a JSON key or the key doesn't match type `A`
    */
  def decodeKey(in: JsonReader): A

  /**
    * Encodes `x` as a JSON key.
    *
    * @param x the value to encode
    * @param out the writer of JSON output
    * @throws JsonWriterException if `x` can't be encoded as RFC-8259 compliant JSON
    */
  def encodeKey(x: A, out: JsonWriter): Unit
}