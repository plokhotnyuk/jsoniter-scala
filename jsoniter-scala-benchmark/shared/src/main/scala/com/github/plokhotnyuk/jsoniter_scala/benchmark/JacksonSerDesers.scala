package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.fasterxml.jackson.annotation.{JsonFormat, JsonInclude, JsonTypeInfo}
import tools.jackson.core.json.{JsonFactoryBuilder, JsonWriteFeature}
import tools.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import tools.jackson.databind.jsontype.NamedType
import tools.jackson.core._
import tools.jackson.databind._
import tools.jackson.databind.json.JsonMapper
import tools.jackson.datatype.jsr310.{JavaTimeFeature, JavaTimeModule}
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.StdSerializer
import tools.jackson.module.blackbird.BlackbirdModule
import tools.jackson.module.scala.deser.{ImmutableBitSetDeserializer, MutableBitSetDeserializer}
import tools.jackson.module.scala.{BitSetDeserializerModule, ClassTagExtensions, DefaultScalaModule}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import tools.jackson.datatype.jsr310.ser.YearSerializer
import java.time.Year
import scala.collection.immutable.BitSet
import scala.collection.mutable

object JacksonSerDesers {

  private[this] def createJacksonMapper(escapeNonAscii: Boolean = false, indentOutput: Boolean = false,
      booleanAsString: Boolean = false, byteArrayAsBase64String: Boolean = true): ObjectMapper with ClassTagExtensions = {
    val jsonFactory = new JsonFactoryBuilder()
      .configure(JsonWriteFeature.ESCAPE_NON_ASCII, escapeNonAscii)
      .configure(JsonWriteFeature.ESCAPE_FORWARD_SLASHES, false)
      .configure(StreamReadFeature.USE_FAST_DOUBLE_PARSER, true)
      .configure(StreamWriteFeature.USE_FAST_DOUBLE_WRITER, true)
      .configure(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER, true)
      .configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION, true)
      .streamReadConstraints(StreamReadConstraints.builder()
        .maxNumberLength(Int.MaxValue) // WARNING: It is an unsafe option for open systems
        .maxNestingDepth(Int.MaxValue) // WARNING: It is an unsafe option for open systems
        .build())
      .streamWriteConstraints(StreamWriteConstraints.builder()
        .maxNestingDepth(Int.MaxValue) // WARNING: It is an unsafe option for open systems
        .build())
      .build()
    val builder = JsonMapper.builder(jsonFactory)
      .addMixIn(classOf[GeoJSON.GeoJSON], classOf[MixIn])
      .addMixIn(classOf[GeoJSON.Geometry], classOf[MixIn])
      .registerSubtypes(
        new NamedType(classOf[GeoJSON.Point], "Point"),
        new NamedType(classOf[GeoJSON.MultiPoint], "MultiPoint"),
        new NamedType(classOf[GeoJSON.LineString], "LineString"),
        new NamedType(classOf[GeoJSON.MultiLineString], "MultiLineString"),
        new NamedType(classOf[GeoJSON.Polygon], "Polygon"),
        new NamedType(classOf[GeoJSON.MultiPolygon], "MultiPolygon"),
        new NamedType(classOf[GeoJSON.GeometryCollection], "GeometryCollection"),
        new NamedType(classOf[GeoJSON.Feature], "Feature"),
        new NamedType(classOf[GeoJSON.FeatureCollection], "FeatureCollection"))
      .addModule(DefaultScalaModule)
      .addModule(BitSetDeserializerModule)
      .addModule(new JavaTimeModule)
      .withConfigOverride(classOf[Year], x => x.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING)))
      .addModule(new SimpleModule()
        .addDeserializer(classOf[BitSet], ImmutableBitSetDeserializer)
        .addDeserializer(classOf[mutable.BitSet], MutableBitSetDeserializer)
        .addSerializer(classOf[SuitADT], new SuitADTSerializer)
        .addSerializer(classOf[SuitEnum], new SuiteEnumSerializer)
        .addDeserializer(classOf[SuitADT], new SuitADTDeserializer)
        .addDeserializer(classOf[SuitEnum], new SuiteEnumDeserializer)
      )
      .addModule(new BlackbirdModule)
      .configure(SerializationFeature.INDENT_OUTPUT, indentOutput)
      .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true)
      .configure(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true)
      .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
      .changeDefaultPropertyInclusion(_
        .withValueInclusion(JsonInclude.Include.NON_EMPTY)
        .withContentInclusion(JsonInclude.Include.NON_EMPTY))
      .defaultPrettyPrinter {
        val indenter = new DefaultIndenter("  ", "\n")
        new DefaultPrettyPrinter().withObjectIndenter(indenter).withArrayIndenter(indenter)
      }
    if (booleanAsString) {
      builder.addModule(new SimpleModule().addSerializer(classOf[Boolean], new StringifiedBooleanSerializer))
    }
    if (!byteArrayAsBase64String) {
      builder.addModule(new SimpleModule().addSerializer(classOf[Array[Byte]], new ByteArraySerializer))
    }
    builder.build() :: ClassTagExtensions
  }

  val jacksonMapper: ObjectMapper with ClassTagExtensions = createJacksonMapper()
  val jacksonPrettyMapper: ObjectMapper with ClassTagExtensions = createJacksonMapper(indentOutput = true)
  val jacksonEscapeNonAsciiMapper: ObjectMapper with ClassTagExtensions = createJacksonMapper(escapeNonAscii = true)
  val jacksonByteArrayMapper: ObjectMapper with ClassTagExtensions = createJacksonMapper(byteArrayAsBase64String = false)
  val jacksonBooleanAsStringMapper: ObjectMapper with ClassTagExtensions = createJacksonMapper(booleanAsString = true)
}

class ByteArraySerializer extends StdSerializer[Array[Byte]](classOf[Array[Byte]]) {
  override def serialize(value: Array[Byte], gen: JsonGenerator, ctxt: SerializationContext): Unit = {
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

class StringifiedBooleanSerializer extends ValueSerializer[Boolean] {
  override def serialize(x: Boolean, jgen: JsonGenerator, ctxt: SerializationContext): Unit = jgen.writeString(x.toString)
}

class SuiteEnumSerializer extends ValueSerializer[SuitEnum] {
  override def serialize(x: SuitEnum, jg: JsonGenerator, ctxt: SerializationContext): Unit = jg.writeString(x.toString)
}

class SuiteEnumDeserializer extends ValueDeserializer[SuitEnum] {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): SuitEnum =
    try SuitEnum.withName(jp.getValueAsString) catch {
      case _: NoSuchElementException => ctxt.handleUnexpectedToken(classOf[SuitEnum], jp).asInstanceOf[SuitEnum]
    }
}

class SuitADTSerializer extends ValueSerializer[SuitADT] {
  override def serialize(x: SuitADT, jg: JsonGenerator, ctxt: SerializationContext): Unit = jg.writeString(x.toString)
}

class SuitADTDeserializer extends ValueDeserializer[SuitADT] {
  private[this] val m = Map(
    "Hearts" -> Hearts,
    "Spades" -> Spades,
    "Diamonds" -> Diamonds,
    "Clubs" -> Clubs)

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): SuitADT =
    m.get(jp.getValueAsString) match {
      case s: Some[SuitADT] => s.value
      case _ => ctxt.handleUnexpectedToken(classOf[SuitADT], jp).asInstanceOf[SuitADT]
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
abstract class MixIn