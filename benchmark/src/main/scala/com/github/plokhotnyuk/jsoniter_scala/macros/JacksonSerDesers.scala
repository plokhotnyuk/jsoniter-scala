package com.github.plokhotnyuk.jsoniter_scala.macros

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.{JsonFactory, JsonGenerator, JsonParser, JsonToken}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum

import scala.collection.immutable.BitSet
import scala.collection.mutable

object JacksonSerDesers {
  val jacksonMapper: ObjectMapper with ScalaObjectMapper = new ObjectMapper(new JsonFactory {
    disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
  }) with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    registerModule(new SimpleModule()
      .addSerializer(classOf[BitSet], new BitSetSerializer)
      .addSerializer(classOf[mutable.BitSet], new MutableBitSetSerializer)
      .addSerializer(classOf[Array[Byte]], new ByteArraySerializer)
      .addSerializer(classOf[SuitEnum], new SuitEnumSerializer)
      .addDeserializer(classOf[SuitEnum], new SuitEnumDeserializer))
    registerModule(new JavaTimeModule)
    registerModule(new AfterburnerModule)
    configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
    configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
    setSerializationInclusion(Include.NON_EMPTY)
  }
}

class BitSetSerializer extends StdSerializer[BitSet](classOf[BitSet]) {
  override def serialize(value: BitSet, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartArray()
    if (!isEmpty(provider, value)) value.foreach(gen.writeNumber)
    gen.writeEndArray()
  }

  override def isEmpty(provider: SerializerProvider, value: BitSet): Boolean = value.isEmpty
}

class MutableBitSetSerializer extends StdSerializer[mutable.BitSet](classOf[mutable.BitSet]) {
  override def serialize(value: mutable.BitSet, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartArray()
    if (!isEmpty(provider, value)) value.foreach(gen.writeNumber)
    gen.writeEndArray()
  }

  override def isEmpty(provider: SerializerProvider, value: mutable.BitSet): Boolean = value.isEmpty
}

class ByteArraySerializer extends StdSerializer[Array[Byte]](classOf[Array[Byte]]) {
  override def serialize(value: Array[Byte], gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartArray()
    if (!isEmpty(provider, value)) {
      val l = value.length
      var i = 0
      while (i < l) {
        gen.writeNumber(value(i))
        i += 1
      }
    }
    gen.writeEndArray()
  }

  override def isEmpty(provider: SerializerProvider, value: Array[Byte]): Boolean = value.isEmpty
}

class SuitEnumSerializer extends JsonSerializer[SuitEnum] {
  override def serialize(value: SuitEnum, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeString(value.toString)
}

class SuitEnumDeserializer extends JsonDeserializer[SuitEnum] {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): SuitEnum =
    jp.getCurrentToken match {
      case JsonToken.VALUE_STRING => SuitEnum.withName(jp.getValueAsString)
      case _ => ctxt.handleUnexpectedToken(classOf[SuitEnum], jp).asInstanceOf[SuitEnum]
    }
}