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

package com.github.plokhotnyuk.jsoniter_scala

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import scala.{specialized => sp}

package object core {
  private[this] final val readerPool: ThreadLocal[JsonReader] = new ThreadLocal[JsonReader] {
    override def initialValue(): JsonReader = new JsonReader
  }
  private[this] final val writerPool: ThreadLocal[JsonWriter] = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = new JsonWriter
  }
  private[this] final val emptyBuf: Array[Byte] = new Array[Byte](0)

  /**
    * Parses UTF-8 encoded JSON from an input stream into a value of type `A`.
    *
    * BEWARE: Nested calls of non-reentrant routines in the same thread fail. Use `...Reentrant` routines for all but
    * the innermost call.
    *
    * @tparam A the type of the value to parse
    * @param in the input stream to parse from
    * @param config the reader configuration
    * @param codec the JSON value codec
    * @return the parsed value
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.io.IOException if reading from `in` fails
    * @throws java.lang.NullPointerException if `codec`, `in`, or `config` is `null`
    */
  def readFromStream[@sp A](in: InputStream, config: ReaderConfig = ReaderConfig)
                           (implicit codec: JsonValueCodec[A]): A = {
    if (in eq null) throw new NullPointerException
    readerPool.get.read(codec, in, config)
  }

  /**
    * Parses UTF-8 encoded JSON from an input stream into a value of type `A`.
    *
    * Allocates a new reader, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to parse
    * @param in the input stream to parse from
    * @param config the reader configuration
    * @param codec the JSON value codec
    * @return the parsed value
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.io.IOException if reading from `in` fails
    * @throws java.lang.NullPointerException if `codec`, `in`, or `config` is `null`
    */
  def readFromStreamReentrant[@sp A](in: InputStream, config: ReaderConfig = ReaderConfig)
                                    (implicit codec: JsonValueCodec[A]): A = {
    if (in eq null) throw new NullPointerException
    new JsonReader(buf = emptyBuf, charBuf = new Array[Char](config.preferredCharBufSize))
      .withoutBufReallocation().read(codec, in, config)
  }

  /**
    * Parses UTF-8 encoded JSON values separated by whitespace from an input stream into values of type `A`.
    *
    * Passes each parsed value to `f`.
    *
    * BEWARE: Nested calls of non-reentrant routines in the same thread fail. Use `...Reentrant` routines for all but
    * the innermost call.
    *
    * @tparam A the type of the value to parse
    * @param in the input stream to parse from
    * @param config the reader configuration
    * @param f the function that receives each parsed value and returns `false` to stop
    * @param codec the JSON value codec
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.io.IOException if reading from `in` fails
    * @throws java.lang.NullPointerException if `codec`, `in`, `config`, or `f` is `null`
    * @throws java.lang.Throwable if `f` throws it
    */
  def scanJsonValuesFromStream[@sp A](in: InputStream, config: ReaderConfig = ReaderConfig)(f: A => Boolean)
                                     (implicit codec: JsonValueCodec[A]): Unit = {
    if ((in eq null) || (f eq null)) throw new NullPointerException
    readerPool.get.scanValueStream(codec, in, config)(f)
  }

  /**
    * Parses UTF-8 encoded JSON values separated by whitespace from an input stream into values of type `A`.
    *
    * Passes each parsed value to `f`.
    *
    * Allocates a new reader, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to parse
    * @param in the input stream to parse from
    * @param config the reader configuration
    * @param f the function that receives each parsed value and returns `false` to stop
    * @param codec the JSON value codec
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.io.IOException if reading from `in` fails
    * @throws java.lang.NullPointerException if `codec`, `in`, `config`, or `f` is `null`
    * @throws java.lang.Throwable if `f` throws it
    */
  def scanJsonValuesFromStreamReentrant[@sp A](in: InputStream, config: ReaderConfig = ReaderConfig)(f: A => Boolean)
                                              (implicit codec: JsonValueCodec[A]): Unit = {
    if ((in eq null) || (f eq null)) throw new NullPointerException
    new JsonReader(buf = emptyBuf, charBuf = new Array[Char](config.preferredCharBufSize))
      .withoutBufReallocation().scanValueStream(codec, in, config)(f)
  }

  /**
    * Parses a UTF-8 encoded JSON array from an input stream into its values of type `A`.
    *
    * Passes each parsed value to `f`.
    *
    * @tparam A the type of the value to parse
    * @param in the input stream to parse from
    * @param config the reader configuration
    * @param f the function that receives each parsed value and returns `false` to stop
    * @param codec the JSON value codec
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.io.IOException if reading from `in` fails
    * @throws java.lang.NullPointerException if `codec`, `in`, `config`, or `f` is `null`
    * @throws java.lang.Throwable if `f` throws it
    */
  def scanJsonArrayFromStream[@sp A](in: InputStream, config: ReaderConfig = ReaderConfig)(f: A => Boolean)
                                    (implicit codec: JsonValueCodec[A]): Unit = {
    if ((in eq null) || (f eq null)) throw new NullPointerException
    readerPool.get.scanArray(codec, in, config)(f)
  }

  /**
    * Parses a UTF-8 encoded JSON array from an input stream into its values of type `A`.
    *
    * Passes each parsed value to `f`.
    *
    * Allocates a new reader, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to parse
    * @param in the input stream to parse from
    * @param config the reader configuration
    * @param f the function that receives each parsed value and returns `false` to stop
    * @param codec the JSON value codec
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.io.IOException if reading from `in` fails
    * @throws java.lang.NullPointerException if `codec`, `in`, `config`, or `f` is `null`
    * @throws java.lang.Throwable if `f` throws it
    */
  def scanJsonArrayFromStreamReentrant[@sp A](in: InputStream, config: ReaderConfig = ReaderConfig)(f: A => Boolean)
                                             (implicit codec: JsonValueCodec[A]): Unit = {
    if ((in eq null) || (f eq null)) throw new NullPointerException
    new JsonReader(buf = emptyBuf, charBuf = new Array[Char](config.preferredCharBufSize))
      .withoutBufReallocation().scanArray(codec, in, config)(f)
  }

  /**
    * Parses UTF-8 encoded JSON from a byte array into a value of type `A`.
    *
    * @tparam A the type of the value to parse
    * @param buf the byte array to parse from
    * @param config the reader configuration
    * @param codec the JSON value codec
    * @return the parsed value
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.lang.NullPointerException if `codec`, `buf`, or `config` is `null`
    */
  def readFromArray[@sp A](buf: Array[Byte], config: ReaderConfig = ReaderConfig)
                          (implicit codec: JsonValueCodec[A]): A =
    readerPool.get.read(codec, buf, 0, buf.length, config)

  /**
    * Parses UTF-8 encoded JSON from a byte array into a value of type `A`.
    *
    * Allocates a new reader, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to parse
    * @param buf the byte array to parse from
    * @param config the reader configuration
    * @param codec the JSON value codec
    * @return the parsed value
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.lang.NullPointerException if `codec`, `buf`, or `config` is `null`
    */
  def readFromArrayReentrant[@sp A](buf: Array[Byte], config: ReaderConfig = ReaderConfig)
                                   (implicit codec: JsonValueCodec[A]): A =
    new JsonReader(buf = buf, charBuf = new Array[Char](Math.min(config.preferredCharBufSize, buf.length >> 2)))
      .withoutBufReallocation().read(codec, buf, 0, buf.length, config)

  /**
    * Parses UTF-8 encoded JSON from a byte array into a value of type `A`.
    *
    * @tparam A the type of the value to parse
    * @param buf the byte array to parse from
    * @param from the start index in `buf` (inclusive)
    * @param to the end index in `buf` (exclusive)
    * @param config the reader configuration
    * @param codec the JSON value codec
    * @return the parsed value
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.lang.NullPointerException if `codec`, `buf`, or `config` is `null`
    * @throws java.lang.ArrayIndexOutOfBoundsException if `from` or `to` is outside of `buf`, or `from` is greater than
    *                                                  `to`
    */
  def readFromSubArray[@sp A](buf: Array[Byte], from: Int, to: Int, config: ReaderConfig = ReaderConfig)
                             (implicit codec: JsonValueCodec[A]): A = {
    if (to > buf.length || to < 0)
      throw new ArrayIndexOutOfBoundsException("`to` should be positive and not greater than `buf` length")
    if (from > to || from < 0)
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `to`")
    readerPool.get.read(codec, buf, from, to, config)
  }

  /**
    * Parses UTF-8 encoded JSON from a byte array into a value of type `A`.
    *
    * Allocates a new reader, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to parse
    * @param buf the byte array to parse from
    * @param from the start index in `buf` (inclusive)
    * @param to the end index in `buf` (exclusive)
    * @param config the reader configuration
    * @param codec the JSON value codec
    * @return the parsed value
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.lang.NullPointerException if `codec`, `buf`, or `config` is `null`
    * @throws java.lang.ArrayIndexOutOfBoundsException if `from` or `to` is outside of `buf`, or `from` is greater than
    *                                                  `to`
    */
  def readFromSubArrayReentrant[@sp A](buf: Array[Byte], from: Int, to: Int, config: ReaderConfig = ReaderConfig)
                                      (implicit codec: JsonValueCodec[A]): A = {
    if (to > buf.length || to < 0)
      throw new ArrayIndexOutOfBoundsException("`to` should be positive and not greater than `buf` length")
    if (from > to || from < 0)
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `to`")
    new JsonReader(buf = buf, charBuf = new Array[Char](Math.min(config.preferredCharBufSize, to - from >> 2)))
      .withoutBufReallocation().read(codec, buf, from, to, config)
  }

  /**
    * Parses UTF-8 encoded JSON from a byte buffer into a value of type `A`.
    *
    * Parses from the current position of `bbuf` until the value ends or the limit is reached, then sets the position
    * after the last read byte.
    *
    * @tparam A the type of the value to parse
    * @param bbuf the byte buffer to parse from
    * @param config the reader configuration
    * @param codec the JSON value codec
    * @return the parsed value
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.lang.NullPointerException if `codec`, `bbuf`, or `config` is `null`
    */
  def readFromByteBuffer[@sp A](bbuf: ByteBuffer, config: ReaderConfig = ReaderConfig)
                               (implicit codec: JsonValueCodec[A]): A =
    readerPool.get.read(codec, bbuf, config)

  /**
    * Parses UTF-8 encoded JSON from a byte buffer into a value of type `A`.
    *
    * Parses from the current position of `bbuf` until the value ends or the limit is reached, then sets the position
    * after the last read byte.
    *
    * Allocates a new reader, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to parse
    * @param bbuf the byte buffer to parse from
    * @param config the reader configuration
    * @param codec the JSON value codec
    * @return the parsed value
    * @throws JsonReaderException if the input has malformed UTF-8 bytes or illegal JSON, the JSON doesn't match type
    *                             `A`, or the input ends unexpectedly
    * @throws java.lang.NullPointerException if `codec`, `bbuf`, or `config` is `null`
    */
  def readFromByteBufferReentrant[@sp A](bbuf: ByteBuffer, config: ReaderConfig = ReaderConfig)
                                        (implicit codec: JsonValueCodec[A]): A =
    new JsonReader(buf = emptyBuf, charBuf = new Array[Char](Math.min(config.preferredCharBufSize, bbuf.limit() - bbuf.position() >> 2)))
      .withoutBufReallocation().read(codec, bbuf, config)

  /**
    * Parses JSON from a string into a value of type `A`.
    *
    * @tparam A the type of the value to parse
    * @param s the string to parse from
    * @param config the reader configuration
    * @param codec the JSON value codec
    * @return the parsed value
    * @throws JsonReaderException if the input has illegal JSON, the JSON doesn't match type `A`, or the input ends
    *                             unexpectedly
    * @throws java.lang.NullPointerException if `codec`, `s`, or `config` is `null`
    */
  def readFromString[A](s: String, config: ReaderConfig = ReaderConfig)(implicit codec: JsonValueCodec[A]): A =
    readerPool.get.read(codec, s, config)

  /**
    * Parses JSON from a string into a value of type `A`.
    *
    * Allocates a new reader, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to parse
    * @param s the string to parse from
    * @param config the reader configuration
    * @param codec the JSON value codec
    * @return the parsed value
    * @throws JsonReaderException if the input has illegal JSON, the JSON doesn't match type `A`, or the input ends
    *                             unexpectedly
    * @throws java.lang.NullPointerException if `codec`, `s`, or `config` is `null`
    */
  def readFromStringReentrant[A](s: String, config: ReaderConfig = ReaderConfig)(implicit codec: JsonValueCodec[A]): A = {
    if ((s eq null) || (codec eq null) || (config eq null)) throw new NullPointerException
    new JsonReader(buf = emptyBuf, charBuf = new Array[Char](Math.min(config.preferredCharBufSize, s.length >> 2)))
      .withoutBufReallocation().read(codec, s, config)
  }

  /**
    * Serializes `x` to the given output stream as UTF-8 encoded JSON.
    *
    * BEWARE: Nested calls of non-reentrant routines in the same thread fail. Use `...Reentrant` routines for all but
    * the innermost call.
    *
    * Doesn't need a buffered output stream. Tune the size of internal buffers with the `preferredBufSize` option of
    * `config`.
    *
    * @tparam A the type of the value to serialize
    * @param x the value to serialize
    * @param out the output stream to serialize to
    * @param config the writer configuration
    * @param codec the JSON value codec
    * @throws JsonWriterException if `x` has strings with illegal surrogate pairs, or non-finite `Float` or `Double`
    *                             values
    * @throws java.io.IOException if writing to `out` fails
    * @throws java.lang.NullPointerException if `x`, `codec`, `out`, or `config` is `null`
    */
  def writeToStream[@sp A](x: A, out: OutputStream, config: WriterConfig = WriterConfig)
                          (implicit codec: JsonValueCodec[A]): Unit = {
    if (out eq null) throw new NullPointerException
    writerPool.get.write(codec, x, out, config)
  }

  /**
    * Serializes `x` to the given output stream as UTF-8 encoded JSON.
    *
    * Allocates a new writer, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to serialize
    * @param x the value to serialize
    * @param out the output stream to serialize to
    * @param config the writer configuration
    * @param codec the JSON value codec
    * @throws JsonWriterException if `x` has strings with illegal surrogate pairs, or non-finite `Float` or `Double`
    *                             values
    * @throws java.io.IOException if writing to `out` fails
    * @throws java.lang.NullPointerException if `x`, `codec`, `out`, or `config` is `null`
    */
  def writeToStreamReentrant[@sp A](x: A, out: OutputStream, config: WriterConfig = WriterConfig)
                                   (implicit codec: JsonValueCodec[A]): Unit = {
    if (out eq null) throw new NullPointerException
    new JsonWriter(buf = emptyBuf, limit = 0).withoutBufReallocation().write(codec, x, out, config)
  }

  /**
    * Serializes `x` to a new byte array as UTF-8 encoded JSON.
    *
    * BEWARE: Nested calls of non-reentrant routines in the same thread fail. Use `...Reentrant` routines for all but
    * the innermost call.
    *
    * To avoid reallocation of internal buffers, set the `preferredBufSize` option of `config` to the expected output
    * size if it exceeds 32 KB.
    *
    * @tparam A the type of the value to serialize
    * @param x the value to serialize
    * @param config the writer configuration
    * @param codec the JSON value codec
    * @return the JSON bytes
    * @throws JsonWriterException if `x` has strings with illegal surrogate pairs, or non-finite `Float` or `Double`
    *                             values
    * @throws java.lang.NullPointerException if `x`, `codec`, or `config` is `null`
    */
  def writeToArray[@sp A](x: A, config: WriterConfig = WriterConfig)
                         (implicit codec: JsonValueCodec[A]): Array[Byte] =
    writerPool.get.write(codec, x, config)

  /**
    * Serializes `x` to a new byte array as UTF-8 encoded JSON.
    *
    * Allocates a new writer, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to serialize
    * @param x the value to serialize
    * @param config the writer configuration
    * @param codec the JSON value codec
    * @return the JSON bytes
    * @throws JsonWriterException if `x` has strings with illegal surrogate pairs, or non-finite `Float` or `Double`
    *                             values
    * @throws java.lang.NullPointerException if `x`, `codec`, or `config` is `null`
    */
  def writeToArrayReentrant[@sp A](x: A, config: WriterConfig = WriterConfig)
                                  (implicit codec: JsonValueCodec[A]): Array[Byte] =
    new JsonWriter(buf = emptyBuf, limit = 0).withoutBufReallocation().write(codec, x, config)

  /**
    * Serializes `x` to the given byte array slice as UTF-8 encoded JSON.
    *
    * BEWARE: Nested calls of non-reentrant routines in the same thread fail. Use `...Reentrant` routines for all but
    * the innermost call.
    *
    * @tparam A the type of the value to serialize
    * @param x the value to serialize
    * @param buf the byte array to serialize to
    * @param from the start index in `buf` (inclusive)
    * @param to the end index in `buf` (exclusive)
    * @param config the writer configuration
    * @param codec the JSON value codec
    * @return the index after the last byte written to `buf`
    * @throws JsonWriterException if `x` has strings with illegal surrogate pairs, or non-finite `Float` or `Double`
    *                             values
    * @throws java.lang.NullPointerException if `x`, `codec`, `buf`, or `config` is `null`
    * @throws java.lang.ArrayIndexOutOfBoundsException if `from` or `to` is outside of `buf`, `from` is greater than
    *                                                  `to`, or the output doesn't fit before `to`
    */
  def writeToSubArray[@sp A](x: A, buf: Array[Byte], from: Int, to: Int, config: WriterConfig = WriterConfig)
                            (implicit codec: JsonValueCodec[A]): Int = {
    if (to > buf.length) // also checks that `buf` is not null before any serialization
      throw new ArrayIndexOutOfBoundsException("`to` should be positive and not greater than `buf` length")
    if (from > to || from < 0)
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `to`")
    writerPool.get.write(codec, x, buf, from, to, config)
  }

  /**
    * Serializes `x` to the given byte array slice as UTF-8 encoded JSON.
    *
    * Allocates a new writer, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to serialize
    * @param x the value to serialize
    * @param buf the byte array to serialize to
    * @param from the start index in `buf` (inclusive)
    * @param to the end index in `buf` (exclusive)
    * @param config the writer configuration
    * @param codec the JSON value codec
    * @return the index after the last byte written to `buf`
    * @throws JsonWriterException if `x` has strings with illegal surrogate pairs, or non-finite `Float` or `Double`
    *                             values
    * @throws java.lang.NullPointerException if `x`, `codec`, `buf`, or `config` is `null`
    * @throws java.lang.ArrayIndexOutOfBoundsException if `from` or `to` is outside of `buf`, `from` is greater than
    *                                                  `to`, or the output doesn't fit before `to`
    */
  def writeToSubArrayReentrant[@sp A](x: A, buf: Array[Byte], from: Int, to: Int, config: WriterConfig = WriterConfig)
                                     (implicit codec: JsonValueCodec[A]): Int = {
    if (to > buf.length) // also checks that `buf` is not null before any serialization
      throw new ArrayIndexOutOfBoundsException("`to` should be positive and not greater than `buf` length")
    if (from > to || from < 0)
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `to`")
    new JsonWriter(buf = buf).write(codec, x, buf, from, to, config)
  }

  /**
    * Serializes `x` to the given byte buffer as UTF-8 encoded JSON.
    *
    * Serializes from the current position of `bbuf` up to its limit, then sets the position after the last written
    * byte.
    *
    * BEWARE: Nested calls of non-reentrant routines in the same thread fail. Use `...Reentrant` routines for all but
    * the innermost call.
    *
    * Uses internal buffers when `bbuf` is a [[java.nio.DirectByteBuffer]], so set the `preferredBufSize` option of
    * `config` to the expected output size if it exceeds 32 KB.
    *
    * @tparam A the type of the value to serialize
    * @param x the value to serialize
    * @param bbuf the byte buffer to serialize to
    * @param config the writer configuration
    * @param codec the JSON value codec
    * @throws JsonWriterException if `x` has strings with illegal surrogate pairs, or non-finite `Float` or `Double`
    *                             values
    * @throws java.lang.NullPointerException if `x`, `codec`, `bbuf`, or `config` is `null`
    * @throws java.nio.ReadOnlyBufferException if `bbuf` is read-only
    * @throws java.nio.BufferOverflowException if the output doesn't fit in `bbuf`
    */
  def writeToByteBuffer[@sp A](x: A, bbuf: ByteBuffer, config: WriterConfig = WriterConfig)
                              (implicit codec: JsonValueCodec[A]): Unit =
    writerPool.get.write(codec, x, bbuf, config)

  /**
    * Serializes `x` to the given byte buffer as UTF-8 encoded JSON.
    *
    * Serializes from the current position of `bbuf` up to its limit, then sets the position after the last written
    * byte.
    *
    * Allocates a new writer, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to serialize
    * @param x the value to serialize
    * @param bbuf the byte buffer to serialize to
    * @param config the writer configuration
    * @param codec the JSON value codec
    * @throws JsonWriterException if `x` has strings with illegal surrogate pairs, or non-finite `Float` or `Double`
    *                             values
    * @throws java.lang.NullPointerException if `x`, `codec`, `bbuf`, or `config` is `null`
    * @throws java.nio.ReadOnlyBufferException if `bbuf` is read-only
    * @throws java.nio.BufferOverflowException if the output doesn't fit in `bbuf`
    */
  def writeToByteBufferReentrant[@sp A](x: A, bbuf: ByteBuffer, config: WriterConfig = WriterConfig)
                                       (implicit codec: JsonValueCodec[A]): Unit =
    new JsonWriter(buf = emptyBuf, limit = 0).withoutBufReallocation().write(codec, x, bbuf, config)

  /**
    * Serializes `x` to a JSON string.
    *
    * BEWARE: Nested calls of non-reentrant routines in the same thread fail. Use `...Reentrant` routines for all but
    * the innermost call.
    *
    * To avoid reallocation of internal buffers, set the `preferredBufSize` option of `config` to the expected output
    * size if it exceeds 32 KB.
    *
    * @tparam A the type of the value to serialize
    * @param x the value to serialize
    * @param config the writer configuration
    * @param codec the JSON value codec
    * @return the JSON string
    * @throws JsonWriterException if `x` has strings with illegal surrogate pairs, or non-finite `Float` or `Double`
    *                             values
    * @throws java.lang.NullPointerException if `x`, `codec`, or `config` is `null`
    */
  def writeToString[@sp A](x: A, config: WriterConfig = WriterConfig)(implicit codec: JsonValueCodec[A]): String =
    writerPool.get.writeToString(codec, x, config)

  /**
    * Serializes `x` to a JSON string.
    *
    * Allocates a new writer, so it is slower than the pooled version but safe to nest in the same thread.
    *
    * @tparam A the type of the value to serialize
    * @param x the value to serialize
    * @param config the writer configuration
    * @param codec the JSON value codec
    * @return the JSON string
    * @throws JsonWriterException if `x` has strings with illegal surrogate pairs, or non-finite `Float` or `Double`
    *                             values
    * @throws java.lang.NullPointerException if `x`, `codec`, or `config` is `null`
    */
  def writeToStringReentrant[@sp A](x: A, config: WriterConfig = WriterConfig)(implicit codec: JsonValueCodec[A]): String =
    new JsonWriter(buf = emptyBuf, limit = 0).writeToStringWithoutBufReallocation(codec, x, config)
}