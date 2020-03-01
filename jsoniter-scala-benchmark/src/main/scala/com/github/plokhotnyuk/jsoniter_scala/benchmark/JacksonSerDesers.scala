package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.fasterxml.jackson.core.{JsonFactory, JsonFactoryBuilder, JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum

object JacksonSerDesers {
  def createJacksonMapper: ObjectMapper with ScalaObjectMapper = {
    val jsonFactory = new JsonFactoryBuilder()
      .configure(JsonFactory.Feature.INTERN_FIELD_NAMES, false)
      .build()
    new ObjectMapper(jsonFactory) with ScalaObjectMapper {
      registerModule(DefaultScalaModule)
      registerModule(new SimpleModule()
        .addSerializer(classOf[SuitADT], new SuitADTSerializer)
        .addSerializer(classOf[SuitEnum], new SuitEnumSerializer)
        .addDeserializer(classOf[SuitADT], new SuitADTDeserializer)
        .addDeserializer(classOf[SuitEnum], new SuitEnumDeserializer))
      registerModule(new JavaTimeModule)
      registerModule(new Jdk8Module)
      registerModule(new AfterburnerModule)
      configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
      configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      configure(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true)
      configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
      configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
      setSerializationInclusion(Include.NON_EMPTY)
      setDefaultPrettyPrinter {
        val indenter = new DefaultIndenter("  ", "\n")
        val prettyPrinter = new DefaultPrettyPrinter()
        prettyPrinter.indentArraysWith(indenter)
        prettyPrinter.indentObjectsWith(indenter)
        prettyPrinter
      }
    }
  }

  val jacksonMapper: ObjectMapper with ScalaObjectMapper = createJacksonMapper
  val jacksonPrettyMapper: ObjectMapper with ScalaObjectMapper = createJacksonMapper
  jacksonPrettyMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
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

class StringifiedBooleanSerializer extends JsonSerializer[Boolean] {
  override def serialize(value: Boolean, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeString(value.toString)
}

class SuitEnumSerializer extends JsonSerializer[SuitEnum] {
  override def serialize(value: SuitEnum, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeString(value.toString)
}

class SuitEnumDeserializer extends JsonDeserializer[SuitEnum] {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): SuitEnum =
    if (jp.getCurrentToken != VALUE_STRING) ctxt.handleUnexpectedToken(classOf[SuitEnum], jp).asInstanceOf[SuitEnum]
    else SuitEnum.withName(jp.getValueAsString)
}

class SuitADTSerializer extends JsonSerializer[SuitADT] {
  override def serialize(value: SuitADT, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeString(value.toString)
}

class SuitADTDeserializer extends JsonDeserializer[SuitADT] {
  private[this] val suite = Map(
    "Hearts" -> Hearts,
    "Spades" -> Spades,
    "Diamonds" -> Diamonds,
    "Clubs" -> Clubs)

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): SuitADT =
    if (jp.getCurrentToken != VALUE_STRING) ctxt.handleUnexpectedToken(classOf[SuitADT], jp).asInstanceOf[SuitADT]
    else {
      val s = jp.getValueAsString
      suite.getOrElse(s, ctxt.handleWeirdStringValue(classOf[SuitADT], s, "illegal value").asInstanceOf[SuitADT])
    }
}