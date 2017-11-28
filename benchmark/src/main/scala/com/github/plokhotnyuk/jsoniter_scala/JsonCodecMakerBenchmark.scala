package com.github.plokhotnyuk.jsoniter_scala

import java.nio.charset.StandardCharsets._
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.CustomCirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.CustomJacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.CustomPlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.JsonCodecMaker._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations._
import play.api.libs.json.{Json, _}

import scala.collection.immutable.{BitSet, HashMap, HashSet, IntMap, LongMap, Map}
import scala.collection.mutable

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(value = 1, jvmArgs = Array(
  "-server",
  "-Xms1g",
  "-Xmx1g",
  "-XX:NewSize=512m",
  "-XX:MaxNewSize=512m",
  "-XX:InitialCodeCacheSize=256m",
  "-XX:ReservedCodeCacheSize=256m",
  "-XX:-UseBiasedLocking",
  "-XX:+AlwaysPreTouch"
))
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class JsonCodecMakerBenchmark {
  val jacksonMapper: ObjectMapper with ScalaObjectMapper = new ObjectMapper with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    registerModule(new SimpleModule()
      .addSerializer(classOf[BitSet], new BitSetSerializer)
      .addSerializer(classOf[mutable.BitSet], new MutableBitSetSerializer)
      .addDeserializer(classOf[BitSet], new BitSetDeserializer)
      .addDeserializer(classOf[mutable.BitSet], new MutableBitSetDeserializer))
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
    setSerializationInclusion(Include.NON_EMPTY)
  }
  val stacklessExceptionConfig = ReaderConfig(throwParseExceptionWithStackTrace = false)
  val stacklessExceptionWithoutDumpConfig = ReaderConfig(throwParseExceptionWithStackTrace = false, appendHexDumpToParseException = false)
  val missingReqFieldCodec: JsonCodec[MissingReqFields] = make[MissingReqFields](CodecMakerConfig())
  val missingReqFieldFormat: OFormat[MissingReqFields] = Json.format[MissingReqFields]
  val anyRefsCodec: JsonCodec[AnyRefs] = make[AnyRefs](CodecMakerConfig())
  val anyRefsFormat: OFormat[AnyRefs] = Json.format[AnyRefs]
  val arraysCodec: JsonCodec[Arrays] = make[Arrays](CodecMakerConfig())
  val arraysFormat: OFormat[Arrays] = Json.format[Arrays]
  val bitSetsCodec: JsonCodec[BitSets] = make[BitSets](CodecMakerConfig())
  val bitSetsFormat: OFormat[BitSets] = Json.format[BitSets]
  val iterablesCodec: JsonCodec[Iterables] = make[Iterables](CodecMakerConfig())
  val iterablesFormat: OFormat[Iterables] = Json.format[Iterables]
  val mutableIterablesCodec: JsonCodec[MutableIterables] = make[MutableIterables](CodecMakerConfig())
  val mutableIterablesFormat: OFormat[MutableIterables] = Json.format[MutableIterables]
  val mapsCodec: JsonCodec[Maps] = make[Maps](CodecMakerConfig())
  val mapsFormat: OFormat[Maps] = Json.format[Maps]
  val mutableMapsCodec: JsonCodec[MutableMaps] = make[MutableMaps](CodecMakerConfig())
  val mutableMapsFormat: OFormat[MutableMaps] = Json.format[MutableMaps]
  val intAndLongMapsCodec: JsonCodec[IntAndLongMaps] = make[IntAndLongMaps](CodecMakerConfig())
  val intAndLongMapsFormat: OFormat[IntAndLongMaps] = Json.format[IntAndLongMaps]
  val primitivesCodec: JsonCodec[Primitives] = make[Primitives](CodecMakerConfig())
  val primitivesFormat: OFormat[Primitives] = Json.format[Primitives]
  val extractFieldsCodec: JsonCodec[ExtractFields] = make[ExtractFields](CodecMakerConfig())
  val extractFieldsFormat: OFormat[ExtractFields] = Json.format[ExtractFields]
  val adtCodec: JsonCodec[AdtBase] = make[AdtBase](CodecMakerConfig())
  val adtFormat: OFormat[AdtBase] = v14
  val missingReqFieldJson: Array[Byte] = """{}""".getBytes
  val anyRefsJson: Array[Byte] = """{"s":"s","bd":1,"os":"os"}""".getBytes
  val arraysJson: Array[Byte] = """{"aa":[[1,2,3],[4,5,6]],"a":[7]}""".getBytes
  val bitSetsJson: Array[Byte] = """{"bs":[1,2,3],"mbs":[4,5,6]}""".getBytes
  val iterablesJson: Array[Byte] = """{"l":["1","2","3"],"s":[4,5,6],"ls":[[1,2],[]]}""".getBytes
  val mutableIterablesJson: Array[Byte] = """{"l":["1","2","3"],"s":[4,5,6],"ls":[[1,2],[]]}""".getBytes
  val mapsJson: Array[Byte] = """{"m":{"1":1.1,"2":2.2},"mm":{"1":{"3":3.3},"2":{}}}""".getBytes
  val mutableMapsJson: Array[Byte] = """{"m":{"2":2.2,"1":1.1},"mm":{"2":{},"1":{"3":3.3}}}""".getBytes
  val intAndLongMapsJson: Array[Byte] = """{"m":{"1":1.1,"2":2.2},"mm":{"2":{},"1":{"3":3.3}}}""".getBytes
  val primitivesJson: Array[Byte] = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":1.1,"f":2.5}""".getBytes
  val extractFieldsJson: Array[Byte] =
    """{"i1":["1","2"],"s":"s","i2":{"m":[[1,2],[3,4]],"f":true},"l":1,"i3":{"1":1.1,"2":2.2}}""".getBytes
  val adtJson: Array[Byte] =
    """{"type":"C","l":{"type":"A","a":1},"r":{"type":"B","b":"VVV"}}""".getBytes
  val anyRefsObj: AnyRefs = AnyRefs("s", 1, Some("os"))
  val arraysObj: Arrays = Arrays(Array(Array(1, 2, 3), Array(4, 5, 6)), Array(BigInt(7)))
  val bitSetsObj: BitSets = BitSets(BitSet(1, 2, 3), mutable.BitSet(4, 5, 6))
  val iterablesObj: Iterables = Iterables(Vector("1", "2", "3"), Set(4, 5, 6), List(HashSet(1, 2), HashSet()))
  val mutableIterablesObj: MutableIterables = MutableIterables(mutable.ArrayBuffer("1", "2", "3"), mutable.TreeSet(4, 5, 6),
    mutable.ResizableArray(mutable.Set(1, 2), mutable.Set()))
  val mapsObj: Maps = Maps(HashMap("1" -> 1.1, "2" -> 2.2), Map(1 -> HashMap(3L -> 3.3), 2 -> HashMap.empty[Long, Double]))
  val mutableMapsObj: MutableMaps = MutableMaps(mutable.HashMap("1" -> 1.1, "2" -> 2.2),
    mutable.Map(1 -> mutable.OpenHashMap(3L -> 3.3), 2 -> mutable.OpenHashMap.empty[Long, Double]))
  val intAndLongMapsObj: IntAndLongMaps = IntAndLongMaps(IntMap(1 -> 1.1, 2 -> 2.2),
    mutable.LongMap(1L -> LongMap(3L -> 3.3), 2L -> LongMap.empty[Double]))
  val primitivesObj: Primitives = Primitives(1, 2, 3, 4, bl = true, ch = 'x', 1.1, 2.5f)
  val extractFieldsObj: ExtractFields = ExtractFields("s", 1L)
  val adtObj: AdtBase = C(A(1), B("VVV"))
  val preallocatedBuf: Array[Byte] = new Array[Byte](100000)

  @Benchmark
  def missingReqFieldCirce(): String =
    decode[MissingReqFields](new String(missingReqFieldJson, UTF_8)).fold(_.getMessage, _ => null)

  @Benchmark
  def missingReqFieldJackson(): String =
    try {
      jacksonMapper.readValue[MissingReqFields](missingReqFieldJson).toString // toString() should not be called
    } catch {
      case ex: MismatchedInputException => ex.getMessage
    }

  @Benchmark
  def missingReqFieldJsoniter(): String =
    try {
      JsonReader.read(missingReqFieldCodec, missingReqFieldJson).toString // toString() should not be called
    } catch {
      case ex: JsonParseException => ex.getMessage
    }

  @Benchmark
  def missingReqFieldJsoniterStackless(): String =
    try {
      JsonReader.read(missingReqFieldCodec, missingReqFieldJson, stacklessExceptionConfig).toString // toString() should not be called
    } catch {
      case ex: JsonParseException => ex.getMessage
    }

  @Benchmark
  def missingReqFieldJsoniterStacklessNoDump(): String =
    try {
      JsonReader.read(missingReqFieldCodec, missingReqFieldJson, stacklessExceptionWithoutDumpConfig).toString // toString() should not be called
    } catch {
      case ex: JsonParseException => ex.getMessage
    }

  @Benchmark
  def missingReqFieldPlay(): String =
    try {
      Json.parse(missingReqFieldJson).as[MissingReqFields](missingReqFieldFormat).toString // toString() should not be called
    } catch {
      case ex: JsResultException => ex.getMessage
    }

  @Benchmark
  def readAnyRefsCirce(): AnyRefs = decode[AnyRefs](new String(anyRefsJson, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readAnyRefsJackson(): AnyRefs = jacksonMapper.readValue[AnyRefs](anyRefsJson)

  @Benchmark
  def readAnyRefsJsoniter(): AnyRefs = JsonReader.read(anyRefsCodec, anyRefsJson)

  @Benchmark
  def readAnyRefsPlay(): AnyRefs = Json.parse(anyRefsJson).as[AnyRefs](anyRefsFormat)

  @Benchmark
  def readArraysCirce(): Arrays = decode[Arrays](new String(arraysJson, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readArraysJackson(): Arrays = jacksonMapper.readValue[Arrays](arraysJson)

  @Benchmark
  def readArraysJsoniter(): Arrays = JsonReader.read(arraysCodec, arraysJson)

  @Benchmark
  def readArraysPlay(): Arrays = Json.parse(arraysJson).as[Arrays](arraysFormat)

/* FIXME: Circe doesn't support parsing of bitsets
  @Benchmark
  def readBitSetsCirce(): BitSets = decode[BitSets](new String(bitSetsJson, StandardCharsets.UTF_8)).fold(throw _, x => x)
*/

  @Benchmark
  def readBitSetsJackson(): BitSets = jacksonMapper.readValue[BitSets](bitSetsJson)

  @Benchmark
  def readBitSetsJsoniter(): BitSets = JsonReader.read(bitSetsCodec, bitSetsJson)

  @Benchmark
  def readBitSetsPlay(): BitSets = Json.parse(bitSetsJson).as[BitSets](bitSetsFormat)

  @Benchmark
  def readIterablesCirce(): Iterables = decode[Iterables](new String(iterablesJson, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readIterablesJackson(): Iterables = jacksonMapper.readValue[Iterables](iterablesJson)

  @Benchmark
  def readIterablesJsoniter(): Iterables = JsonReader.read(iterablesCodec, iterablesJson)

  @Benchmark
  def readIterablesPlay(): Iterables = Json.parse(iterablesJson).as[Iterables](iterablesFormat)

  @Benchmark
  def readMutableIterablesCirce(): MutableIterables = decode[MutableIterables](new String(mutableIterablesJson, UTF_8)).fold(throw _, x => x)

/* FIXME: Jackson-module-scala doesn't support parsing of tree sets
  @Benchmark
  def readMutableIterablesJackson(): MutableIterables = jacksonMapper.readValue[MutableIterables](mutableIterablesJson)
*/

  @Benchmark
  def readMutableIterablesJsoniter(): MutableIterables = JsonReader.read(mutableIterablesCodec, mutableIterablesJson)

  @Benchmark
  def readMutableIterablesPlay(): MutableIterables = Json.parse(mutableIterablesJson).as[MutableIterables](mutableIterablesFormat)

  @Benchmark
  def readMapsCirce(): Maps = decode[Maps](new String(mapsJson, UTF_8)) .fold(throw _, x => x)

  @Benchmark
  def readMapsJackson(): Maps = jacksonMapper.readValue[Maps](mapsJson)

  @Benchmark
  def readMapsJsoniter(): Maps = JsonReader.read(mapsCodec, mapsJson)

  @Benchmark
  def readMapsPlay(): Maps = Json.parse(mapsJson).as[Maps](mapsFormat)

/* FIXME: Circe doesn't support parsing of mutable maps
  @Benchmark
  def readMutableMapsCirce(): MutableMaps = decode[MutableMaps](new String(mutableMapsJson, StandardCharsets.UTF_8)).fold(throw _, x => x)
*/

  @Benchmark
  def readMutableMapsJackson(): MutableMaps = jacksonMapper.readValue[MutableMaps](mutableMapsJson)

  @Benchmark
  def readMutableMapsJsoniter(): MutableMaps = JsonReader.read(mutableMapsCodec, mutableMapsJson)

  @Benchmark
  def readMutableMapsPlay(): MutableMaps = Json.parse(mutableMapsJson).as[MutableMaps](mutableMapsFormat)

/* FIXME: Circe doesn't support parsing of int & long maps
  @Benchmark
  def readIntAndLongMapsCirce(): IntAndLongMaps = decode[IntAndLongMaps](new String(intAndLongMapsJson, StandardCharsets.UTF_8)).fold(throw _, x => x)
*/

/* FIXME: Jackson-module-scala doesn't support parsing of int & long maps
  @Benchmark
  def readIntAndLongMapsJackson(): IntAndLongMaps = jacksonMapper.readValue[IntAndLongMaps](intAndLongMapsJson)
*/

  @Benchmark
  def readIntAndLongMapsJsoniter(): IntAndLongMaps = JsonReader.read(intAndLongMapsCodec, intAndLongMapsJson)

  @Benchmark
  def readIntAndLongMapsPlay(): IntAndLongMaps = Json.parse(intAndLongMapsJson).as[IntAndLongMaps](intAndLongMapsFormat)

  @Benchmark
  def readPrimitivesCirce(): Primitives = decode[Primitives](new String(primitivesJson, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readPrimitivesJackson(): Primitives = jacksonMapper.readValue[Primitives](primitivesJson)

  @Benchmark
  def readPrimitivesJsoniter(): Primitives = JsonReader.read(primitivesCodec, primitivesJson)

  @Benchmark
  def readPrimitivesPlay(): Primitives = Json.parse(primitivesJson).as[Primitives](primitivesFormat)

  @Benchmark
  def readExtractFieldsCirce(): ExtractFields = decode[ExtractFields](new String(extractFieldsJson, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readExtractFieldsJackson(): ExtractFields = jacksonMapper.readValue[ExtractFields](extractFieldsJson)

  @Benchmark
  def readExtractFieldsJsoniter(): ExtractFields = JsonReader.read(extractFieldsCodec, extractFieldsJson)

  @Benchmark
  def readExtractFieldsPlay(): ExtractFields = Json.parse(extractFieldsJson).as[ExtractFields](extractFieldsFormat)

  @Benchmark
  def readAdtCirce(): AdtBase = decode[AdtBase](new String(adtJson, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readAdtJackson(): AdtBase = jacksonMapper.readValue[AdtBase](adtJson)

  @Benchmark
  def readAdtJsoniter(): AdtBase = JsonReader.read(adtCodec, adtJson)

  @Benchmark
  def readAdtPlay(): AdtBase = Json.parse(adtJson).as[AdtBase](adtFormat)

  @Benchmark
  def readGoogleMapsAPICirce(): GoogleMapsAPI.DistanceMatrix = decode[GoogleMapsAPI.DistanceMatrix](new String(GoogleMapsAPI.json, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readGoogleMapsAPIJackson(): GoogleMapsAPI.DistanceMatrix = jacksonMapper.readValue[GoogleMapsAPI.DistanceMatrix](GoogleMapsAPI.json)

  @Benchmark
  def readGoogleMapsAPIJsoniter(): GoogleMapsAPI.DistanceMatrix = JsonReader.read(GoogleMapsAPI.codec, GoogleMapsAPI.json)

  @Benchmark
  def readGoogleMapsAPIPlay(): GoogleMapsAPI.DistanceMatrix = Json.parse(GoogleMapsAPI.json).as[GoogleMapsAPI.DistanceMatrix](GoogleMapsAPI.format)

  @Benchmark
  def readTwitterAPICirce(): Seq[TwitterAPI.Tweet] = decode[Seq[TwitterAPI.Tweet]](new String(TwitterAPI.json, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readTwitterAPIJackson(): Seq[TwitterAPI.Tweet] = jacksonMapper.readValue[Seq[TwitterAPI.Tweet]](TwitterAPI.json)

  @Benchmark
  def readTwitterAPIJsoniter(): Seq[TwitterAPI.Tweet] = JsonReader.read(TwitterAPI.codec, TwitterAPI.json)

  @Benchmark
  def readTwitterAPIPlay(): Seq[TwitterAPI.Tweet] = Json.parse(TwitterAPI.json).as[Seq[TwitterAPI.Tweet]](TwitterAPI.format)

  @Benchmark
  def writeAnyRefsCirce(): Array[Byte] = anyRefsObj.asJson.noSpaces.getBytes(UTF_8)

  @Benchmark
  def writeAnyRefsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(anyRefsObj)

  @Benchmark
  def writeAnyRefsJsoniter(): Array[Byte] = JsonWriter.write(anyRefsCodec, anyRefsObj)

  @Benchmark
  def writeAnyRefsJsoniterPrealloc(): Int = JsonWriter.write(anyRefsCodec, anyRefsObj, preallocatedBuf, 0)

  @Benchmark
  def writeAnyRefsPlay(): Array[Byte] = Json.toBytes(Json.toJson(anyRefsObj)(anyRefsFormat))

  @Benchmark
  def writeArraysCirce(): Array[Byte] = arraysObj.asJson.noSpaces.getBytes(UTF_8)

  @Benchmark
  def writeArraysJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(arraysObj)

  @Benchmark
  def writeArraysJsoniter(): Array[Byte] = JsonWriter.write(arraysCodec, arraysObj)

  @Benchmark
  def writeArraysPlay(): Array[Byte] = Json.toBytes(Json.toJson(arraysObj)(arraysFormat))

/* FIXME: Circe doesn't support writing of bitsets
  @Benchmark
  def writeBitSetsCirce(): Array[Byte] = bitSetsObj.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
*/

  @Benchmark
  def writeBitSetsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(bitSetsObj)

  @Benchmark
  def writeBitSetsJsoniter(): Array[Byte] = JsonWriter.write(bitSetsCodec, bitSetsObj)

  @Benchmark
  def writeBitSetsPlay(): Array[Byte] = Json.toBytes(Json.toJson(bitSetsObj)(bitSetsFormat))

  @Benchmark
  def writeIterablesCirce(): Array[Byte] = iterablesObj.asJson.noSpaces.getBytes(UTF_8)

  @Benchmark
  def writeIterablesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(iterablesObj)

  @Benchmark
  def writeIterablesJsoniter(): Array[Byte] = JsonWriter.write(iterablesCodec, iterablesObj)

  @Benchmark
  def writeIterablesPlay(): Array[Byte] = Json.toBytes(Json.toJson(iterablesObj)(iterablesFormat))

  @Benchmark
  def writeMutableIterablesCirce(): Array[Byte] = mutableIterablesObj.asJson.noSpaces.getBytes(UTF_8)

  @Benchmark
  def writeMutableIterablesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(mutableIterablesObj)

  @Benchmark
  def writeMutableIterablesJsoniter(): Array[Byte] = JsonWriter.write(mutableIterablesCodec, mutableIterablesObj)

  @Benchmark
  def writeMutableIterablesPlay(): Array[Byte] = Json.toBytes(Json.toJson(mutableIterablesObj)(mutableIterablesFormat))

  @Benchmark
  def writeMapsCirce(): Array[Byte] = mapsObj.asJson.noSpaces.getBytes(UTF_8)

  @Benchmark
  def writeMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(mapsObj)

  @Benchmark
  def writeMapsJsoniter(): Array[Byte] = JsonWriter.write(mapsCodec, mapsObj)

  @Benchmark
  def writeMapsPlay(): Array[Byte] = Json.toBytes(Json.toJson(mapsObj)(mapsFormat))

  @Benchmark
  def writeMutableMapsCirce(): Array[Byte] = mutableMapsObj.asJson.noSpaces.getBytes(UTF_8)

  @Benchmark
  def writeMutableMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(mutableMapsObj)

  @Benchmark
  def writeMutableMapsJsoniter(): Array[Byte] = JsonWriter.write(mutableMapsCodec, mutableMapsObj)

  @Benchmark
  def writeMutableMapsPlay(): Array[Byte] = Json.toBytes(Json.toJson(mutableMapsObj)(mutableMapsFormat))

/* FIXME: Circe doesn't support writing of int & long maps
  @Benchmark
  def writeIntAndLongMapsCirce(): Array[Byte] = intAndLongMapsObj.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
*/

  @Benchmark
  def writeIntAndLongMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(intAndLongMapsObj)

  @Benchmark
  def writeIntAndLongMapsJsoniter(): Array[Byte] = JsonWriter.write(intAndLongMapsCodec, intAndLongMapsObj)

  @Benchmark
  def writeIntAndLongMapsPlay(): Array[Byte] = Json.toBytes(Json.toJson(intAndLongMapsObj)(intAndLongMapsFormat))

  @Benchmark
  def writePrimitivesCirce(): Array[Byte] = primitivesObj.asJson.noSpaces.getBytes(UTF_8)

  @Benchmark
  def writePrimitivesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(primitivesObj)

  @Benchmark
  def writePrimitivesJsoniter(): Array[Byte] = JsonWriter.write(primitivesCodec, primitivesObj)

  @Benchmark
  def writePrimitivesJsoniterPrealloc(): Int = JsonWriter.write(primitivesCodec, primitivesObj, preallocatedBuf, 0)

  @Benchmark
  def writePrimitivesPlay(): Array[Byte] = Json.toBytes(Json.toJson(primitivesObj)(primitivesFormat))

  @Benchmark
  def writeAdtCirce(): Array[Byte] = adtObj.asJson.noSpaces.getBytes(UTF_8)

  @Benchmark
  def writeAdtJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(adtObj)

  @Benchmark
  def writeAdtJsoniter(): Array[Byte] = JsonWriter.write(adtCodec, adtObj)

  @Benchmark
  def writeAdtPlay(): Array[Byte] = Json.toBytes(Json.toJson(adtObj)(adtFormat))

  @Benchmark
  def writeGoogleMapsAPICirce(): Array[Byte] = GoogleMapsAPI.obj.asJson.noSpaces.getBytes(UTF_8)

  @Benchmark
  def writeGoogleMapsAPIJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(GoogleMapsAPI.obj)

  @Benchmark
  def writeGoogleMapsAPIJsoniter(): Array[Byte] = JsonWriter.write(GoogleMapsAPI.codec, GoogleMapsAPI.obj)

  @Benchmark
  def writeGoogleMapsAPIJsoniterPrealloc(): Int = JsonWriter.write(GoogleMapsAPI.codec, GoogleMapsAPI.obj, preallocatedBuf, 0)

  @Benchmark
  def writeGoogleMapsAPIPlay(): Array[Byte] = Json.toBytes(Json.toJson(GoogleMapsAPI.obj)(GoogleMapsAPI.format))

  @Benchmark
  def writeTwitterAPICirce(): Array[Byte] = TwitterAPI.obj.asJson.noSpaces.getBytes(UTF_8)

  @Benchmark
  def writeTwitterAPIJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(TwitterAPI.obj)

  @Benchmark
  def writeTwitterAPIJsoniter(): Array[Byte] = JsonWriter.write(TwitterAPI.codec, TwitterAPI.obj)

  @Benchmark
  def writeTwitterAPIJsoniterPrealloc(): Int = JsonWriter.write(TwitterAPI.codec, TwitterAPI.obj, preallocatedBuf, 0)

  @Benchmark
  def writeTwitterAPIPlay(): Array[Byte] = Json.toBytes(Json.toJson(TwitterAPI.obj)(TwitterAPI.format))
}

case class MissingReqFields(
  @com.fasterxml.jackson.annotation.JsonProperty(required = true) s: String,
  @com.fasterxml.jackson.annotation.JsonProperty(required = true) i: Int)

case class AnyRefs(s: String, bd: BigDecimal, os: Option[String])

case class Arrays(aa: Array[Array[Int]], a: Array[BigInt])

case class Iterables(l: Vector[String], s: Set[Int], ls: List[HashSet[Long]])

case class MutableIterables(l: mutable.ArrayBuffer[String], s: mutable.TreeSet[Int], ls: mutable.ResizableArray[mutable.Set[Long]])

case class BitSets(bs: BitSet, mbs: mutable.BitSet)

case class Maps(m: HashMap[String, Double], mm: Map[Int, HashMap[Long, Double]])

case class MutableMaps(m: mutable.HashMap[String, Double], mm: mutable.Map[Int, mutable.OpenHashMap[Long, Double]])

case class IntAndLongMaps(m: IntMap[Double], mm: mutable.LongMap[LongMap[Double]])

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

case class ExtractFields(s: String, l: Long)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[A], name = "A"),
  new Type(value = classOf[B], name = "B"),
  new Type(value = classOf[C], name = "C")))
sealed trait AdtBase extends Product with Serializable

case class A(a: Int) extends AdtBase

case class B(b: String) extends AdtBase

case class C(l: AdtBase, r: AdtBase) extends AdtBase
