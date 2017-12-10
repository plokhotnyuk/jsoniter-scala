package com.github.plokhotnyuk.jsoniter_scala.core

abstract class JsonCodec[A] {
  def nullValue: A

  def decode(in: JsonReader, default: A): A

  def encode(x: A, out: JsonWriter): Unit
}