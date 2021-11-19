package com.github.plokhotnyuk.jsoniter_scala.core

import scala.{specialized => sp}

/**
  * A `JsonCodec[A]` instance is a universal codec for JSON values and keys.
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