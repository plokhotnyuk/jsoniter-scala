package com.github.plokhotnyuk.jsoniter_scala.core

trait JsonKeyCodec[A] {
  def decodeKey(in: JsonReader): A

  def encodeKey(x: A, out: JsonWriter): Unit
}