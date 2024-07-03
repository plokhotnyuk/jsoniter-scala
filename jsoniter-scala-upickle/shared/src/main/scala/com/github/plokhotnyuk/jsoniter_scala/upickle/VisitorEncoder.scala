package com.github.plokhotnyuk.jsoniter_scala.upickle

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import upickle.core.Transformer

abstract class VisitorEncoder[I](t: Transformer[I]) extends JsonValueCodec[I] {
  override def decodeValue(in: JsonReader, default: I): I =
    throw new UnsupportedOperationException("Codec only supports encoding")

  override def encodeValue(x: I, out: JsonWriter): Unit =
    t.transform(x, new JsonWriterVisitor(out))

  override def nullValue: I = null.asInstanceOf[I]
}
