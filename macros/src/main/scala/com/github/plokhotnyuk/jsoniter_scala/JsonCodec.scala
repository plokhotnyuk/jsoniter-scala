package com.github.plokhotnyuk.jsoniter_scala

abstract class JsonCodec[A] {
  def default: A

  def decode(in: JsonReader, default: A): A

  def encode(x: A, out: JsonWriter): Unit
}