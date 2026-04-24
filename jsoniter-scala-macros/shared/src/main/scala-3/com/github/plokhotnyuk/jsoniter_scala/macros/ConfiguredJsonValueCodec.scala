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

package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._

/**
 * An extension of `JsonValueCodec[A]` that could be used to derive codecs using the `... derives ...` syntax.
 *
 * A compile-time configuration can be provided by an `inline given CodecMakerConfig` value, which needs to be visible
 * in the scope, and defined/imported before the `... derives ...` syntax usage.
 *
 * BEWARE: Using of this class requires _runtime_ scope for the `jsoniter-scala-macros` dependency.
 *
 * Also, to avoid overhead in runtime use it only for top-level and big commonly used data structures.
 */
trait ConfiguredJsonValueCodec[A] extends JsonValueCodec[A]

object ConfiguredJsonValueCodec:
  inline def derived[A](using inline config: CodecMakerConfig = CodecMakerConfig): ConfiguredJsonValueCodec[A] =
    new ConfiguredJsonValueCodecWrapper(JsonCodecMaker.make[A](config))

private[macros] class ConfiguredJsonValueCodecWrapper[A](impl: JsonValueCodec[A]) extends ConfiguredJsonValueCodec[A] {
  @inline
  def decodeValue(in: JsonReader, default: A): A = impl.decodeValue(in, default)

  @inline
  def encodeValue(x: A, out: JsonWriter): Unit = impl.encodeValue(x, out)

  @inline
  def nullValue: A = impl.nullValue
}