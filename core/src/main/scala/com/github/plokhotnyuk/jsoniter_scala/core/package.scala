package com.github.plokhotnyuk.jsoniter_scala

import java.io.{InputStream, OutputStream}

package object core {
  private final val readerConfig = new ReaderConfig
  private final val readerPool: ThreadLocal[JsonReader] = new ThreadLocal[JsonReader] {
    override def initialValue(): JsonReader = new JsonReader
  }
  private final val writerConfig = new WriterConfig
  private final val writerPool: ThreadLocal[JsonWriter] = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = new JsonWriter
  }

  /**
    * Deserialize JSON content encoded in UTF-8 from an input stream into a value of given `A` type
    * with default parsing options that maximize description of error.
    *
    * Use custom configuration to turn on raising of stackless exceptions and/or turn off a hex dump printing
    * to the error message.
    *
    * @tparam A type of the value to parse
    * @param in the input stream to parse from
    * @param codec a codec for the given `A` type
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also if a low-level I/O problem (unexpected end of input, network error) occurs
    *                            while some input bytes are expected
    * @throws NullPointerException if the `codec` or `in` is null
    */
  final def read[A](in: InputStream)(implicit codec: JsonCodec[A]): A = {
    if (in eq null) throw new NullPointerException
    readerPool.get.read(codec, in, readerConfig)
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
  final def read[A](in: InputStream, config: ReaderConfig)(implicit codec: JsonCodec[A]): A = {
    if (in eq null) throw new NullPointerException
    readerPool.get.read(codec, in, config)
  }

  /**
    * Deserialize JSON content encoded in UTF-8 from a byte array into a value of given `A` type
    * with default parsing options that maximize description of error.
    *
    * Use custom configuration to turn on raising of stackless exceptions and/or turn off a hex dump printing
    * to the error message.
    *
    * @tparam A type of the value to parse
    * @param buf the byte array to parse from
    * @param codec a codec for the given `A` type
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also in case if end of input is detected while some input bytes are expected
    * @throws NullPointerException If the `codec` or `buf` is null.
    */
  final def read[A](buf: Array[Byte])(implicit codec: JsonCodec[A]): A =
    readerPool.get.read(codec, buf, 0, buf.length, readerConfig)

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
  final def read[A](buf: Array[Byte], config: ReaderConfig)(implicit codec: JsonCodec[A]): A =
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
  final def read[A](buf: Array[Byte], from: Int, to: Int, config: ReaderConfig = readerConfig)
                   (implicit codec: JsonCodec[A]): A = {
    if (to > buf.length || to < 0)
      throw new ArrayIndexOutOfBoundsException("`to` should be positive and not greater than `buf` length")
    if (from > to || from < 0)
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `to`")
    readerPool.get.read(codec, buf, from, to, config)
  }

  /**
    * Serialize the `x` argument to the provided output stream in UTF-8 encoding of JSON format
    * with default configuration options that minimizes output size & time to serialize.
    *
    * @tparam A type of value to serialize
    * @param x the value to serialize
    * @param out an output stream to serialize into
    * @param codec a codec for the given value
    * @throws NullPointerException if the `codec` or `config` is null
    */
  final def write[A](x: A, out: OutputStream)(implicit codec: JsonCodec[A]): Unit = {
    if (out eq null) throw new NullPointerException
    writerPool.get.write(codec, x, out, writerConfig)
  }

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
  final def write[A](x: A, out: OutputStream, config: WriterConfig)(implicit codec: JsonCodec[A]): Unit = {
    if (out eq null) throw new NullPointerException
    writerPool.get.write(codec, x, out, config)
  }

  /**
    * Serialize the `x` argument to a new allocated instance of byte array in UTF-8 encoding of JSON format
    * with default configuration options that minimizes output size & time to serialize.
    *
    * @tparam A type of value to serialize
    * @param x the value to serialize
    * @param codec a codec for the given value
    * @return a byte array with `x` serialized to JSON
    * @throws NullPointerException if the `codec` is null
    */
  final def write[A](x: A)(implicit codec: JsonCodec[A]): Array[Byte] = writerPool.get.write(codec, x, writerConfig)

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
  final def write[A](x: A, config: WriterConfig)(implicit codec: JsonCodec[A]): Array[Byte] =
    writerPool.get.write(codec, x, config)

  /**
    * Serialize the `x` argument to the given instance of byte array in UTF-8 encoding of JSON format
    * that specified by provided configuration options or defaults that minimizes output size & time to serialize.
    *
    * @tparam A type of value to serialize
    * @param x the value to serialize
    * @param buf a byte array where the value should be serialized
    * @param from a position in the byte array from which serialization of the value should start
    * @param config a serialization configuration
    * @param codec a codec for the given value
    * @return number of next position after last byte serialized to `buf`
    * @throws NullPointerException if the `codec`, `buf` or `config` is null
    * @throws ArrayIndexOutOfBoundsException if the `from` is greater than `buf` length or negative,
    *                                        or `buf` length was exceeded during serialization
    */
  final def write[A](x: A, buf: Array[Byte], from: Int, config: WriterConfig = writerConfig)
                    (implicit codec: JsonCodec[A]): Int = {
    if (from > buf.length || from < 0) // also checks that `buf` is not null before any serialization
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `buf` length")
    writerPool.get.write(codec, x, buf, from, config)
  }
}