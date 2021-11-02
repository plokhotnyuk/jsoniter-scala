package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.util.concurrent.ConcurrentHashMap
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.json.JsonWriteFeature
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.fasterxml.jackson.core.{JsonFactory, JsonFactoryBuilder, JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import scala.util.Try

object JacksonSerDesers {
  def createJacksonMapper(escapeNonAscii: Boolean = false,
                          indentOutput: Boolean = false): ObjectMapper with ClassTagExtensions = {
    val jsonFactory = new JsonFactoryBuilder()
      .configure(JsonFactory.Feature.INTERN_FIELD_NAMES, false)
      .configure(JsonWriteFeature.ESCAPE_NON_ASCII, escapeNonAscii)
      .build()
    new ObjectMapper(jsonFactory) with ClassTagExtensions {
      registerModule(DefaultScalaModule)
      registerModule(new SimpleModule()
        .addSerializer(classOf[SuitADT], new SuitADTSerializer)
        .addSerializer(classOf[SuitEnum], new EnumSerializer(SuitEnum))
        .addDeserializer(classOf[SuitADT], new SuitADTDeserializer)
        .addDeserializer(classOf[SuitEnum], new EnumDeserializer(SuitEnum)))
      registerModule(new JavaTimeModule)
      registerModule(new Jdk8Module)
      registerModule(new AfterburnerModule)
      configure(SerializationFeature.INDENT_OUTPUT, indentOutput)
      configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
      configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      configure(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true)
      configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
      configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
      setSerializationInclusion(Include.NON_EMPTY)
      setDefaultPrettyPrinter {
        val indenter = new DefaultIndenter("  ", "\n")
        new DefaultPrettyPrinter().withObjectIndenter(indenter).withArrayIndenter(indenter)
      }
    }
  }

  val jacksonMapper: ObjectMapper with ClassTagExtensions = createJacksonMapper()
  val jacksonPrettyMapper: ObjectMapper with ClassTagExtensions = createJacksonMapper(indentOutput = true)
  val jacksonEscapeNonAsciiMapper: ObjectMapper with ClassTagExtensions = createJacksonMapper(escapeNonAscii = true)
  val jacksonByteArrayMapper: ObjectMapper with ClassTagExtensions = {
    val jm = createJacksonMapper()
    jm.registerModule(new SimpleModule()
      .addSerializer(classOf[Array[Byte]], new ByteArraySerializer))
    jm
  }
}

class ByteArraySerializer extends StdSerializer[Array[Byte]](classOf[Array[Byte]]) {
  override def serialize(value: Array[Byte], gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartArray()
    val l = value.length
    var i = 0
    while (i < l) {
      gen.writeNumber(value(i))
      i += 1
    }
    gen.writeEndArray()
  }
}

class StringifiedBooleanSerializer extends JsonSerializer[Boolean] {
  override def serialize(x: Boolean, jgen: JsonGenerator, spro: SerializerProvider): Unit = jgen.writeString(x.toString)
}

class EnumSerializer[T <: scala.Enumeration](e: T) extends JsonSerializer[T#Value] {
  override def serialize(x: T#Value, jg: JsonGenerator, spro: SerializerProvider): Unit = jg.writeString(x.toString)
}

class EnumDeserializer[T <: scala.Enumeration](e: T) extends JsonDeserializer[T#Value] {
  private[this] val ec = new ConcurrentHashMap[String, T#Value]

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): T#Value = Try {
    val s = jp.getValueAsString
    var x = ec.get(s)
    if (x eq null) {
      x = e.values.iterator.find(_.toString == s).get
      ec.put(s, x)
    }
    x
  }.getOrElse(ctxt.handleUnexpectedToken(classOf[T#Value], jp).asInstanceOf[T#Value])
}

class SuitADTSerializer extends JsonSerializer[SuitADT] {
  override def serialize(x: SuitADT, jg: JsonGenerator, spro: SerializerProvider): Unit = jg.writeString(x.toString)
}

class SuitADTDeserializer extends JsonDeserializer[SuitADT] {
  private[this] val suite = Map(
    "Hearts" -> Hearts,
    "Spades" -> Spades,
    "Diamonds" -> Diamonds,
    "Clubs" -> Clubs)

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): SuitADT =
    Try(suite(jp.getValueAsString)).getOrElse(ctxt.handleUnexpectedToken(classOf[SuitADT], jp).asInstanceOf[SuitADT])
}