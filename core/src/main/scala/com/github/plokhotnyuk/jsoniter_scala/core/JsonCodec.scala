package com.github.plokhotnyuk.jsoniter_scala.core

import scala.{specialized => sp}

trait JsonCodec[@sp A] extends JsonValueCodec[A] with JsonKeyCodec[A]

trait JsonValueCodec[@sp A] {
  def decodeValue(in: JsonReader, default: A): A

  def encodeValue(x: A, out: JsonWriter): Unit

  def nullValue: A
}

trait JsonKeyCodec[@sp A] {
  def decodeKey(in: JsonReader): A

  def encodeKey(x: A, out: JsonWriter): Unit
}