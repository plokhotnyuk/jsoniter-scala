package com.github.plokhotnyuk.jsoniter_scala.core

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec}

/** A version of [[JsonValueCodec]] that uses [[JsonStructuredReader]] for decoding rather than [[JsonReader]].
  */
abstract class JsonStructuredCodec[T] extends JsonValueCodec[T] {
  def decodeValue(in: JsonStructuredReader, default: T): T

  def decodeValue(in: JsonReader, default: T): T =
    decodeValue(new JsonStructuredReader(in), default)
}
