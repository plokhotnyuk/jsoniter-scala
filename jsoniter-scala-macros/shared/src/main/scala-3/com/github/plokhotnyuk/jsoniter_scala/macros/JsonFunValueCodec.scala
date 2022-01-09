package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonKeyCodec, JsonReader, JsonValueCodec, JsonWriter}

class JsonFunValueCodec[T](funNull: T, 
                           funDecode: (JsonReader, T) => T,
                           funEncode: (T, JsonWriter) => Unit
                            ) extends JsonValueCodec[T] {

    override def nullValue: T = funNull

    override def decodeValue(in: JsonReader, default: T): T = {
      funDecode(in, default)
    }

    override def encodeValue(x: T, out: JsonWriter): Unit =
      funEncode(x,out)

}

object JsonFunValueCodec {

   def apply[T](funNull: T, funDecode: (JsonReader, T) => T, funEncode: (T, JsonWriter) => Unit): JsonFunValueCodec[T] =
    new JsonFunValueCodec(funNull, funDecode, funEncode)

}