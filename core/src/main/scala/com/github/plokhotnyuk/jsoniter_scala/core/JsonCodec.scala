package com.github.plokhotnyuk.jsoniter_scala.core

trait JsonCodec[A] extends JsonValueCodec[A] with JsonKeyCodec[A]

trait JsonValueCodec[A] {
  def decodeValue(in: JsonReader, default: A): A

  def encodeValue(x: A, out: JsonWriter): Unit

  def nullValue: A
}

trait JsonKeyCodec[A] {
  def decodeKey(in: JsonReader): A

  def encodeKey(x: A, out: JsonWriter): Unit
}