package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.util.concurrent.ConcurrentHashMap
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.json.JsonWriteFeature
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.fasterxml.jackson.core.{JsonFactory, JsonFactoryBuilder, JsonGenerator, JsonParser, StreamReadFeature, StreamWriteFeature}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.blackbird.BlackbirdModule
import com.fasterxml.jackson.module.scala.deser.{ImmutableBitSetDeserializer, MutableBitSetDeserializer}
import com.fasterxml.jackson.module.scala.{BitSetDeserializerModule, ClassTagExtensions, DefaultScalaModule}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import scala.collection.immutable.BitSet
import scala.collection.mutable

object JacksonSerDesers {
  def createJacksonMapper(escapeNonAscii: Boolean = false,
                          indentOutput: Boolean = false): ObjectMapper with ClassTagExtensions = {
    val jsonFactory = new JsonFactoryBuilder()
      .configure(JsonFactory.Feature.INTERN_FIELD_NAMES, false)
      .configure(JsonWriteFeature.ESCAPE_NON_ASCII, escapeNonAscii)
      .configure(StreamReadFeature.USE_FAST_DOUBLE_PARSER, true)
      .configure(StreamWriteFeature.USE_FAST_DOUBLE_WRITER, true)
      .build()
    new ObjectMapper(jsonFactory) with ClassTagExtensions {
      registerModule(DefaultScalaModule)
      registerModule(BitSetDeserializerModule)
      registerModule(new SimpleModule()
        .addDeserializer(classOf[BitSet], ImmutableBitSetDeserializer)
        .addDeserializer(classOf[mutable.BitSet], MutableBitSetDeserializer)
        .addSerializer(classOf[SuitADT], new SuitADTSerializer)
        .addSerializer(classOf[SuitEnum], new SuiteEnumSerializer)
        .addDeserializer(classOf[SuitADT], new SuitADTDeserializer)
        .addDeserializer(classOf[SuitEnum], new SuiteEnumDeserializer))
      registerModule(new JavaTimeModule)
      registerModule(new Jdk8Module)
      registerModule(new BlackbirdModule)
      configure(SerializationFeature.INDENT_OUTPUT, indentOutput)
      configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
      configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true)
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
    jm.registerModule(new SimpleModule().addSerializer(classOf[Array[Byte]], new ByteArraySerializer))
    jm
  }
  val jacksonBooleanAsStringMapper: ObjectMapper with ClassTagExtensions = {
    val jm = createJacksonMapper()
    jm.registerModule(new SimpleModule().addSerializer(classOf[Boolean], new StringifiedBooleanSerializer))
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

class SuiteEnumSerializer extends JsonSerializer[SuitEnum] {
  override def serialize(x: SuitEnum, jg: JsonGenerator, spro: SerializerProvider): Unit = jg.writeString(x.toString)
}

class SuiteEnumDeserializer extends JsonDeserializer[SuitEnum] {
  private[this] val ec = new ConcurrentHashMap[String, SuitEnum]

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): SuitEnum = {
    val s = jp.getValueAsString
    var x: SuitEnum = null
    if (s ne null) {
      x = ec.get(s)
      if (x eq null) {
        x = SuitEnum.values.iterator.find(_.toString == s).orNull
        ec.put(s, x)
      }
    }
    if (x eq null) ctxt.handleUnexpectedToken(classOf[SuitEnum], jp)
    x
  }
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
    suite.get(jp.getValueAsString) match {
      case s: Some[SuitADT] => s.value
      case _ => ctxt.handleUnexpectedToken(classOf[SuitADT], jp).asInstanceOf[SuitADT]
    }
}