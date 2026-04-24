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
  * A `JsonCodec[A]` instance is a universal codec for JSON values and keys.
  *
  * BEWARE: Minimize usage of `JsonCodec[A]` due to possible creation of the scalability limit on the JDK level,
  * for more info see:
  *
  *   - [[https://www.youtube.com/watch?v=PxcO3WHqmng]]
  *   - [[https://bugs.openjdk.org/browse/JDK-8180450]]
  *   - [[https://github.com/netty/netty/search?q=JDK-8180450&type=issues]]
  *   - [[https://redhatperf.github.io/post/type-check-scalability-issue]]
  *   - [[https://netflixtechblog.com/seeing-through-hardware-counters-a-journey-to-threefold-performance-increase-2721924a2822]]
  */
trait JsonCodec[@sp A] extends JsonValueCodec[A] with JsonKeyCodec[A]

/**
  * A `JsonValueCodec[A]` instance has the ability to decode and encode JSON values to/from values of type `A`,
  * potentially failing with an error if the JSON content does not encode a value of the given type or `A` cannot be
  * encoded properly according to RFC-8259 requirements.
  */
trait JsonValueCodec[@sp A] extends Serializable {
  /**
    * Attempts to decode a value of type `A` from the specified `JsonReader`, but may fail with `JsonReaderException`
    * error if the JSON input does not encode a value of this type.
    *
    * @param in an instance of `JsonReader` which provide an access to the JSON input to parse a JSON value to value of
    *           type `A`
    * @param default the placeholder value provided to initialize some possible local variables
    */
  def decodeValue(in: JsonReader, default: A): A

  /**
    * Encodes the specified value using provided `JsonWriter`, but may fail with `JsonWriterException` if it cannot be
    * encoded properly according to RFC-8259 requirements.
    *
    * @param x the value provided for serialization
    * @param out an instance of `JsonWriter` which provides access to JSON output to serialize the specified value as
    *            a JSON value
    */
  def encodeValue(x: A, out: JsonWriter): Unit

  /**
    * Returns some placeholder value that will be used by the high level code that generates codec instances to
    * initialize local variables for parsed field values which have a codec that was injected using implicit `val`.
    *
    * See the `jsoniter-scala-macros` sub-project code and its tests for usages of `.nullValue` calls.
    */
  def nullValue: A
}

/**
  * A `JsonKeyCodec[A]` instance has the ability to decode and encode JSON keys to/from values of type `A`,
  * potentially failing with an error if the JSON input is not a key or does not encode a value of the given type or
  * `A` cannot be encoded properly according to RFC-8259 requirements.
  */
trait JsonKeyCodec[@sp A] extends Serializable {
  /**
    * Attempts to decode a value of type `A` from the specified `JsonReader`, but may fail with `JsonReaderException`
    * error if the JSON input is not a key or does not encode a value of this type.
    *
    * @param in an instance of `JsonReader` which provide an access to the JSON input to parse a JSON key to value of
    *           type `A`
    */
  def decodeKey(in: JsonReader): A

  /**
    * Encodes the specified value using provided `JsonWriter` as a JSON key, but may fail with `JsonWriterException` if
    * it cannot be encoded properly according to RFC-8259 requirements.
    *
    * @param x the value provided for serialization
    * @param out an instance of `JsonWriter` which provides access to JSON output to serialize the specified value as
    *            a JSON key
    */
  def encodeKey(x: A, out: JsonWriter): Unit
}