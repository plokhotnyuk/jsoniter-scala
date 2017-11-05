package com.github.plokhotnyuk.jsoniter_scala

import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.core.{JsonGenerator, JsonParseException, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}

import scala.collection.immutable.BitSet
import scala.collection.mutable

object CustomJacksonSerDesers {
  class BitSetDeserializer extends StdDeserializer[BitSet](classOf[BitSet]) {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): BitSet =
      if (p.getCurrentToken != START_ARRAY) throw new JsonParseException(p, "expected '[' or null")
      else if (p.nextToken() == END_ARRAY) getNullValue(ctxt)
      else {
        val s = BitSet.newBuilder
        do {
          s += p.getIntValue
        } while (p.nextToken() != END_ARRAY)
        s.result
      }

    override def getNullValue(ctxt: DeserializationContext): BitSet = BitSet.empty
  }

  class BitSetSerializer extends StdSerializer[BitSet](classOf[BitSet]) {
    override def serialize(value: BitSet, gen: JsonGenerator, provider: SerializerProvider): Unit = {
      gen.writeStartArray()
      if (!isEmpty(provider, value)) value.foreach(gen.writeNumber)
      gen.writeEndArray()
    }

    override def isEmpty(provider: SerializerProvider, value: BitSet): Boolean = value.isEmpty
  }

  class MutableBitSetDeserializer extends StdDeserializer[mutable.BitSet](classOf[mutable.BitSet]) {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): mutable.BitSet =
      if (p.getCurrentToken != START_ARRAY) throw new JsonParseException(p, "expected '[' or null")
      else {
        val s = getNullValue(ctxt)
        while (p.nextToken() != END_ARRAY) {
          s.add(p.getIntValue)
        }
        s
      }

    override def getNullValue(ctxt: DeserializationContext): mutable.BitSet = new mutable.BitSet
  }

  class MutableBitSetSerializer extends StdSerializer[mutable.BitSet](classOf[mutable.BitSet]) {
    override def serialize(value: mutable.BitSet, gen: JsonGenerator, provider: SerializerProvider): Unit = {
      gen.writeStartArray()
      if (!isEmpty(provider, value)) value.foreach(gen.writeNumber)
      gen.writeEndArray()
    }

    override def isEmpty(provider: SerializerProvider, value: mutable.BitSet): Boolean = value.isEmpty
  }
}