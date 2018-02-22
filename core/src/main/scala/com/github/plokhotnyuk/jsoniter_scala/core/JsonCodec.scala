package com.github.plokhotnyuk.jsoniter_scala.core

trait JsonCodec[A] extends JsonValueCodec[A] with JsonKeyCodec[A]

trait JsonValueCodec[A] {
  def nullValue: A

  def decode(in: JsonReader, default: A): A

  def encode(x: A, out: JsonWriter): Unit
}

trait JsonKeyCodec[A] {
  def decodeKey(in: JsonReader): A

  def encodeKey(x: A, out: JsonWriter): Unit
}