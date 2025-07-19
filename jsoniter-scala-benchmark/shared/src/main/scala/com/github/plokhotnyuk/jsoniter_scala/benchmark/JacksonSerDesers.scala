package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.fasterxml.jackson.annotation.{JsonFormat, JsonInclude, JsonTypeInfo}
import tools.jackson.core.json.{JsonFactoryBuilder, JsonWriteFeature}
import tools.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter, JsonRecyclerPools}
import tools.jackson.databind.jsontype.NamedType
import tools.jackson.core._
import tools.jackson.databind._
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.StdSerializer
import tools.jackson.module.scala.deser.{ImmutableBitSetDeserializer, MutableBitSetDeserializer}
import tools.jackson.module.scala.{BitSetDeserializerModule, ClassTagExtensions, DefaultScalaModule, ScalaModule}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum

import java.time.Year
import scala.collection.immutable.BitSet
import scala.collection.mutable

object JacksonSerDesers {
  private[this] def createJacksonMapper(escapeNonAscii: Boolean = false, indentOutput: Boolean = false,
      booleanAsString: Boolean = false, byteArrayAsBase64String: Boolean = true): ObjectMapper with ClassTagExtensions = {
    val scalaModuleConfig = new ScalaModule.Config {
      override def shouldSupportScala3Classes(): Boolean = true

      override def shouldDeserializeNullCollectionsAsEmpty(): Boolean = true
    }
    val jsonFactory = new JsonFactoryBuilder()
      .configure(JsonWriteFeature.ESCAPE_NON_ASCII, escapeNonAscii)
      .configure(JsonWriteFeature.ESCAPE_FORWARD_SLASHES, false)
      .configure(StreamWriteFeature.USE_FAST_DOUBLE_WRITER, true)
      .configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION, true)
      .streamReadConstraints(StreamReadConstraints.builder()
        .maxNumberLength(Int.MaxValue) // WARNING: It is an unsafe option for open systems
        .maxNestingDepth(Int.MaxValue) // WARNING: It is an unsafe option for open systems
        .build())
      .streamWriteConstraints(StreamWriteConstraints.builder()
        .maxNestingDepth(Int.MaxValue) // WARNING: It is an unsafe option for open systems
        .build())
      .recyclerPool(JsonRecyclerPools.threadLocalPool)
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
      .withConfigOverride(classOf[Year], x => x.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING)))
      .addModule(new SimpleModule()
        .addDeserializer(classOf[BitSet], new ImmutableBitSetDeserializer(scalaModuleConfig))
        .addDeserializer(classOf[mutable.BitSet], new MutableBitSetDeserializer(scalaModuleConfig))
        .addSerializer(classOf[SuitADT], new SuitADTSerializer)
        .addSerializer(classOf[SuitEnum], new SuiteEnumSerializer)
        .addDeserializer(classOf[SuitADT], new SuitADTDeserializer)
        .addDeserializer(classOf[SuitEnum], new SuiteEnumDeserializer))
      .configure(SerializationFeature.INDENT_OUTPUT, indentOutput)
      .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true)
      .configure(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true)
      .configure(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
      .configure(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID, true)
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
    var i = 0
    while (i < value.length) {
      gen.writeNumber(value(i))
      i += 1
    }
    gen.writeEndArray()
  }
}

class StringifiedBooleanSerializer extends ValueSerializer[Boolean] {
  override def serialize(value: Boolean, gen: JsonGenerator, ctxt: SerializationContext): Unit =
    gen.writeString(value.toString)
}

class SuiteEnumSerializer extends ValueSerializer[SuitEnum] {
  override def serialize(value: SuitEnum, gen: JsonGenerator, ctxt: SerializationContext): Unit =
    gen.writeString(value.toString)
}

class SuiteEnumDeserializer extends ValueDeserializer[SuitEnum] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): SuitEnum =
    try SuitEnum.withName(p.getValueAsString) catch {
      case _: NoSuchElementException => ctxt.handleUnexpectedToken(classOf[SuitEnum], p).asInstanceOf[SuitEnum]
    }
}

class SuitADTSerializer extends ValueSerializer[SuitADT] {
  override def serialize(value: SuitADT, gen: JsonGenerator, ctxt: SerializationContext): Unit =
    gen.writeString(value.toString)
}

class SuitADTDeserializer extends ValueDeserializer[SuitADT] {
  private[this] val m = Map(
    "Hearts" -> Hearts,
    "Spades" -> Spades,
    "Diamonds" -> Diamonds,
    "Clubs" -> Clubs)

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): SuitADT =
    m.get(p.getValueAsString) match {
      case s: Some[SuitADT] => s.value
      case _ => ctxt.handleUnexpectedToken(classOf[SuitADT], p).asInstanceOf[SuitADT]
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
abstract class MixIn