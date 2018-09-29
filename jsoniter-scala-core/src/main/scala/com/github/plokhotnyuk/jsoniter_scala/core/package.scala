package com.github.plokhotnyuk.jsoniter_scala

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer

import scala.{specialized => sp}

// FIXME: revise method naming when the following scalac bug will be fixed: https://github.com/scala/bug/issues/10754
package object core {
  private[this] final val readerConfig = new ReaderConfig
  private[this] final val readerPool: ThreadLocal[JsonReader] = new ThreadLocal[JsonReader] {
    override def initialValue(): JsonReader = new JsonReader
  }
  private[this] final val writerConfig = new WriterConfig
  private[this] final val writerPool: ThreadLocal[JsonWriter] = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = new JsonWriter
  }

  /**
    * Deserialize JSON content encoded in UTF-8 from an input stream into a value of given `A` type.
    *
    * @tparam A type of the value to parse
    * @param in the input stream to parse from
    * @param config a parsing configuration
    * @param codec a codec for the given `A` type
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also if a low-level I/O problem (unexpected end-of-input, network error) occurs
    *                            while some input bytes are expected
    * @throws NullPointerException if the `codec`, `in` or `config` is null
    */
  final def readFromStream[@sp A](in: InputStream, config: ReaderConfig = readerConfig)
                                 (implicit codec: JsonValueCodec[A]): A = {
    if (in eq null) throw new NullPointerException
    readerPool.get.read(codec, in, config)
  }

  /**
    * Deserialize JSON of streaming values encoded in UTF-8 from an input stream into values of given `A` type.
    *
    * All parsed values will be passed to consuming function `f`.
    *
    * @tparam A type of the value to parse
    * @param in the input stream to parse from
    * @param config a parsing configuration
    * @param f a consumer of values, that returns `true` to continue scanning or `false` to complete it
    * @param codec a codec for the given `A` type
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also if a low-level I/O problem (unexpected end-of-input, network error) occurs
    *                            while some input bytes are expected
    * @throws NullPointerException if the `codec`, `in` or `config` is null
    * @throws Throwable if some error was thrown by f() call
    */
  final def scanJsonValuesFromStream[@sp A](in: InputStream, config: ReaderConfig = readerConfig)(f: A => Boolean)
                                           (implicit codec: JsonValueCodec[A]): Unit = {
    if ((in eq null) || (f eq null)) throw new NullPointerException
    readerPool.get.scanValueStream(codec, in, config)(f)
  }

  /**
    * Deserialize JSON array encoded in UTF-8 from an input stream into its values of given `A` type.
    *
    * All parsed values will be passed to consuming function `f`.
    *
    * @tparam A type of the value to parse
    * @param in the input stream to parse from
    * @param config a parsing configuration
    * @param f a consumer of values, that returns `true` to continue scanning or `false` to complete it
    * @param codec a codec for the given `A` type
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also if a low-level I/O problem (unexpected end-of-input, network error) occurs
    *                            while some input bytes are expected
    * @throws NullPointerException if the `codec`, `in` or `config` is null
    * @throws Throwable if some error was thrown by f() call
    */
  final def scanJsonArrayFromStream[@sp A](in: InputStream, config: ReaderConfig = readerConfig)(f: A => Boolean)
                                          (implicit codec: JsonValueCodec[A]): Unit = {
    if ((in eq null) || (f eq null)) throw new NullPointerException
    readerPool.get.scanArray(codec, in, config)(f)
  }

  /**
    * Deserialize JSON content encoded in UTF-8 from a byte array into a value of given `A` type
    * with specified parsing options.
    *
    * @tparam A type of the value to parse
    * @param buf the byte array to parse from
    * @param config a parsing configuration
    * @param codec a codec for the given `A` type
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also in case if end of input is detected while some input bytes are expected
    * @throws NullPointerException if the `codec`, `buf` or `config` is null
    */
  final def readFromArray[@sp A](buf: Array[Byte], config: ReaderConfig = readerConfig)
                                (implicit codec: JsonValueCodec[A]): A =
    readerPool.get.read(codec, buf, 0, buf.length, config)

  /**
    * Deserialize JSON content encoded in UTF-8 from a byte array into a value of given `A` type with
    * specified parsing options or with defaults that maximize description of error.
    *
    * @tparam A type of the value to parse
    * @param buf the byte array to parse from
    * @param from the start position of the provided byte array
    * @param to the position of end of input in the provided byte array
    * @param config a parsing configuration
    * @param codec a codec for the given `A` type
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also in case if end of input is detected while some input bytes are expected
    * @throws NullPointerException if the `codec`, `buf` or `config` is null
    * @throws ArrayIndexOutOfBoundsException if the `to` is greater than `buf` length or negative,
    *                                        or `from` is greater than `to` or negative
    */
  final def readFromSubArray[@sp A](buf: Array[Byte], from: Int, to: Int, config: ReaderConfig = readerConfig)
                                   (implicit codec: JsonValueCodec[A]): A = {
    if (to > buf.length || to < 0)
      throw new ArrayIndexOutOfBoundsException("`to` should be positive and not greater than `buf` length")
    if (from > to || from < 0)
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `to`")
    readerPool.get.read(codec, buf, from, to, config)
  }

  /**
    * Deserialize JSON content encoded in UTF-8 from a byte buffer into a value of given `A` type with
    * specified parsing options or with defaults that maximize description of error.
    *
    * Parsing will start from the current position and will continue until the limit of the provided byte buffer or the
    * value will be parsed before reaching of the limit. In any case the buffer position will be set to the next
    * position after the last read byte.
    *
    * @tparam A type of the value to parse
    * @param bbuf the byte buffer which will be parsed
    * @param config a parsing configuration
    * @param codec a codec for the given `A` type
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for the result type,
    *                            also in case if end of input is detected while some input bytes are expected
    * @throws NullPointerException if the `codec`, `bbuf` or `config` is null
    */
  final def readFromByteBuffer[@sp A](bbuf: ByteBuffer, config: ReaderConfig = readerConfig)
                                     (implicit codec: JsonValueCodec[A]): A =
    readerPool.get.read(codec, bbuf, config)

  /**
    * Serialize the `x` argument to the provided output stream in UTF-8 encoding of JSON format
    * that specified by provided configuration options.
    *
    * @tparam A type of value to serialize
    * @param x the value to serialize
    * @param out an output stream to serialize into
    * @param config a serialization configuration
    * @param codec a codec for the given value
    * @throws NullPointerException if the `codec`, `out` or `config` is null
    */
  final def writeToStream[@sp A](x: A, out: OutputStream, config: WriterConfig = writerConfig)
                                (implicit codec: JsonValueCodec[A]): Unit = {
    if (out eq null) throw new NullPointerException
    writerPool.get.write(codec, x, out, config)
  }

  /**
    * Serialize the `x` argument to a new allocated instance of byte array in UTF-8 encoding of JSON format,
    * that specified by provided configuration options.
    *
    * @tparam A type of value to serialize
    * @param x the value to serialize
    * @param config a serialization configuration
    * @param codec a codec for the given value
    * @return a byte array with `x` serialized to JSON
    * @throws NullPointerException if the `codec` or `config` is null
    */
  final def writeToArray[@sp A](x: A, config: WriterConfig = writerConfig)
                               (implicit codec: JsonValueCodec[A]): Array[Byte] =
    writerPool.get.write(codec, x, config)

  /**
    * Serialize the `x` argument to the given instance of byte array in UTF-8 encoding of JSON format
    * that specified by provided configuration options or defaults that minimizes output size & time to serialize.
    *
    * @tparam A type of value to serialize
    * @param x the value to serialize
    * @param buf a byte array where the value should be serialized
    * @param from a position in the byte array from which serialization of the value should start
    * @param to an exclusive position in the byte array that limits where serialization of the value should stop
    * @param config a serialization configuration
    * @param codec a codec for the given value
    * @return number of next position after last byte serialized to `buf`
    * @throws NullPointerException if the `codec`, `buf` or `config` is null
    * @throws ArrayIndexOutOfBoundsException if the `from` is greater than `to` or negative, if 'to' is greater than
    *                                        `buf` length or `to` limit was exceeded during serialization
    */
  final def writeToSubArray[@sp A](x: A, buf: Array[Byte], from: Int, to: Int, config: WriterConfig = writerConfig)
                                  (implicit codec: JsonValueCodec[A]): Int = {
    if (to > buf.length) // also checks that `buf` is not null before any serialization
      throw new ArrayIndexOutOfBoundsException("`to` should be positive and not greater than `buf` length")
    if (from > to || from < 0)
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `to`")
    writerPool.get.write(codec, x, buf, from, to, config)
  }

  /**
    * Serialize the `x` argument to the given instance of byte buffer in UTF-8 encoding of JSON format
    * that specified by provided configuration options or defaults that minimizes output size & time to serialize.
    *
    * Serialization will start from the current position up to the provided byte buffer limit.
    * On return the byte buffer will has position set to the next position after the last written byte.
    *
    * @tparam A type of value to serialize
    * @param x the value to serialize
    * @param bbuf a byte buffer where the value should be serialized
    * @param config a serialization configuration
    * @param codec a codec for the given value
    * @throws NullPointerException    if the `codec`, `bbuf` or `config` is null
    * @throws ReadOnlyBufferException if the `bbuf` is read-only
    * @throws BufferOverflowException if the `bbuf` limit was exceeded during serialization
    */
  final def writeToByteBuffer[@sp A](x: A, bbuf: ByteBuffer, config: WriterConfig = writerConfig)
                                    (implicit codec: JsonValueCodec[A]): Unit =
    writerPool.get.write(codec, x, bbuf, config)
}