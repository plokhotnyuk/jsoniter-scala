package com.github.plokhotnyuk.jsoniter_scala

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.JsonCodec.materialize
import com.github.plokhotnyuk.jsoniter_scala.CustomJacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.CustomPlayJsonFormats._
import org.openjdk.jmh.annotations._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import play.api.libs.json.{Json, _}

import scala.collection.immutable.{BitSet, HashMap, IntMap, LongMap, Map}
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
  "-XX:+AlwaysPreTouch"))
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class JsonCodecBenchmark {
  val jacksonMapper: ObjectMapper with ScalaObjectMapper = new ObjectMapper with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    registerModule(new SimpleModule()
      .addSerializer(classOf[BitSet], new BitSetSerializer)
      .addSerializer(classOf[mutable.BitSet], new MutableBitSetSerializer)
      .addDeserializer(classOf[BitSet], new BitSetDeserializer)
      .addDeserializer(classOf[mutable.BitSet], new MutableBitSetDeserializer))
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }
  val anyRefsCodec: JsonCodec[AnyRefs] = materialize[AnyRefs]
  val anyRefsFormat: OFormat[AnyRefs] = Json.format[AnyRefs]
  val arraysCodec: JsonCodec[Arrays] = materialize[Arrays]
  val arraysFormat: OFormat[Arrays] = Json.format[Arrays]
  val bitSetsCodec: JsonCodec[BitSets] = materialize[BitSets]
  val bitSetsFormat: OFormat[BitSets] = Json.format[BitSets]
  val iterablesCodec: JsonCodec[Iterables] = materialize[Iterables]
  val iterablesFormat: OFormat[Iterables] = Json.format[Iterables]
  val mapsCodec: JsonCodec[Maps] = materialize[Maps]
  val mapsFormat: OFormat[Maps] = Json.format[Maps]
  val mutableMapsCodec: JsonCodec[MutableMaps] = materialize[MutableMaps]
  val mutableMapsFormat: OFormat[MutableMaps] = Json.format[MutableMaps]
  val intAndLongMapsCodec: JsonCodec[IntAndLongMaps] = materialize[IntAndLongMaps]
  val intAndLongMapsFormat: OFormat[IntAndLongMaps] = Json.format[IntAndLongMaps]
  val primitivesCodec: JsonCodec[Primitives] = materialize[Primitives]
  val primitivesFormat: OFormat[Primitives] = Json.format[Primitives]
  val extractFieldsCodec: JsonCodec[ExtractFields] = materialize[ExtractFields]
  val extractFieldsFormat: OFormat[ExtractFields] = Json.format[ExtractFields]
  val anyRefsJson: Array[Byte] = """{"s":"s","bd":1,"os":"os"}""".getBytes
  val arraysJson: Array[Byte] = """{"aa":[[1,2,3],[4,5,6]],"a":[7]}""".getBytes
  val bitSetsJson: Array[Byte] = """{"bs":[1,2,3],"mbs":[4,5,6]}""".getBytes
  val iterablesJson: Array[Byte] = """{"l":["1","2","3"],"s":[4,5,6],"ls":[[1,2],[]]}""".getBytes
  val mapsJson: Array[Byte] = """{"m":{"1":1.1,"2":2.2},"mm":{"1":{"3":3.3},"2":{}}}""".getBytes
  val mutableMapsJson: Array[Byte] = """{"m":{"2":2.2,"1":1.1},"mm":{"2":{},"1":{"3":3.3}}}""".getBytes
  val intAndLongMapsJson: Array[Byte] = """{"m":{"1":1.1,"2":2.2},"mm":{"2":{},"1":{"3":3.3}}}""".getBytes
  val primitivesJson: Array[Byte] = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":1.1,"f":2.5}""".getBytes
  val extractFieldsJson: Array[Byte] =
    """{"i1":["1","2"],"s":"s","i2":{"m":[[1,2],[3,4]],"f":true},"l":1,"i3":{"1":1.1,"2":2.2}}""".getBytes
  val anyRefsObj: AnyRefs = AnyRefs("s", 1, Some("os"))
  val arraysObj: Arrays = Arrays(Array(Array(1, 2, 3), Array(4, 5, 6)), Array(BigInt(7)))
  val bitSetsObj: BitSets = BitSets(BitSet(1, 2, 3), mutable.BitSet(4, 5, 6))
  val iterablesObj: Iterables = Iterables(List("1", "2", "3"), Set(4, 5, 6), List(Set(1, 2), Set()))
  val mapsObj: Maps = Maps(HashMap("1" -> 1.1, "2" -> 2.2), Map(1 -> HashMap(3L -> 3.3), 2 -> HashMap.empty[Long, Double]))
  val mutableMapsObj: MutableMaps = MutableMaps(mutable.HashMap("1" -> 1.1, "2" -> 2.2),
    mutable.Map(1 -> mutable.OpenHashMap(3L -> 3.3), 2 -> mutable.OpenHashMap.empty[Long, Double]))
  val intAndLongMapsObj: IntAndLongMaps = IntAndLongMaps(IntMap(1 -> 1.1, 2 -> 2.2),
    mutable.LongMap(1L -> LongMap(3L -> 3.3), 2L -> LongMap.empty[Double]))
  val primitivesObj: Primitives = Primitives(1, 2, 3, 4, bl = true, ch = 'x', 1.1, 2.5f)
  val extractFieldsObj: ExtractFields = ExtractFields("s", 1L)

  @Benchmark
  def readAnyRefsCirce(): AnyRefs =
    decode[AnyRefs](new String(anyRefsJson, StandardCharsets.UTF_8)).fold(e => throw new IllegalArgumentException(e), x => x)

  @Benchmark
  def readAnyRefsJackson(): AnyRefs = jacksonMapper.readValue[AnyRefs](anyRefsJson)

  @Benchmark
  def readAnyRefsJsoniter(): AnyRefs = JsonReader.read(anyRefsCodec, anyRefsJson)

  @Benchmark
  def readAnyRefsPlay(): AnyRefs = Json.parse(anyRefsJson).as[AnyRefs](anyRefsFormat)

  @Benchmark
  def readArraysCirce(): Arrays =
    decode[Arrays](new String(arraysJson, StandardCharsets.UTF_8)).fold(e => throw new IllegalArgumentException(e), x => x)

  @Benchmark
  def readArraysJackson(): Arrays = jacksonMapper.readValue[Arrays](arraysJson)

  @Benchmark
  def readArraysJsoniter(): Arrays = JsonReader.read(arraysCodec, arraysJson)

  @Benchmark
  def readArraysPlay(): Arrays = Json.parse(arraysJson).as[Arrays](arraysFormat)

/* FIXME: Circe doesn't support parsing of bitsets
  @Benchmark
  def readBitSetsCirce(): BitSets =
    decode[BitSets](new String(bitSetsJson, StandardCharsets.UTF_8)).fold(e => throw new IllegalArgumentException(e), x => x)
*/

  @Benchmark
  def readBitSetsJackson(): BitSets = jacksonMapper.readValue[BitSets](bitSetsJson)

  @Benchmark
  def readBitSetsJsoniter(): BitSets = JsonReader.read(bitSetsCodec, bitSetsJson)

  @Benchmark
  def readBitSetsPlay(): BitSets = Json.parse(bitSetsJson).as[BitSets](bitSetsFormat)

  @Benchmark
  def readIterablesCirce(): Iterables =
    decode[Iterables](new String(iterablesJson, StandardCharsets.UTF_8)).fold(e => throw new IllegalArgumentException(e), x => x)

  @Benchmark
  def readIterablesJackson(): Iterables = jacksonMapper.readValue[Iterables](iterablesJson)

  @Benchmark
  def readIterablesJsoniter(): Iterables = JsonReader.read(iterablesCodec, iterablesJson)

  @Benchmark
  def readIterablesPlay(): Iterables = Json.parse(iterablesJson).as[Iterables](iterablesFormat)

  @Benchmark
  def readMapsCirce(): Maps =
    decode[Maps](new String(mapsJson, StandardCharsets.UTF_8)).fold(e => throw new IllegalArgumentException(e), x => x)

  @Benchmark
  def readMapsJackson(): Maps = jacksonMapper.readValue[Maps](mapsJson)

  @Benchmark
  def readMapsJsoniter(): Maps = JsonReader.read(mapsCodec, mapsJson)

  @Benchmark
  def readMapsPlay(): Maps = Json.parse(mapsJson).as[Maps](mapsFormat)

/* FIXME: Circe doesn't support parsing of mutable maps
  @Benchmark
  def readMutableMapsCirce(): MutableMaps =
    decode[MutableMaps](new String(mutableMapsJson, StandardCharsets.UTF_8)).fold(e => throw new IllegalArgumentException(e), x => x)
*/

  @Benchmark
  def readMutableMapsJackson(): MutableMaps = jacksonMapper.readValue[MutableMaps](mutableMapsJson)

  @Benchmark
  def readMutableMapsJsoniter(): MutableMaps = JsonReader.read(mutableMapsCodec, mutableMapsJson)

  @Benchmark
  def readMutableMapsPlay(): MutableMaps = Json.parse(mutableMapsJson).as[MutableMaps](mutableMapsFormat)

/* FIXME: Circe doesn't support parsing of int & long maps
  @Benchmark
  def readIntAndLongMapsCirce(): IntAndLongMaps =
    decode[IntAndLongMaps](new String(intAndLongMapsJson, StandardCharsets.UTF_8)).fold(e => throw new IllegalArgumentException(e), x => x)
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
  def readPrimitivesCirce(): Primitives =
    decode[Primitives](new String(primitivesJson, StandardCharsets.UTF_8)).fold(e => throw new IllegalArgumentException(e), x => x)

  @Benchmark
  def readPrimitivesJackson(): Primitives = jacksonMapper.readValue[Primitives](primitivesJson)

  @Benchmark
  def readPrimitivesJsoniter(): Primitives = JsonReader.read(primitivesCodec, primitivesJson)

  @Benchmark
  def readPrimitivesPlay(): Primitives = Json.parse(primitivesJson).as[Primitives](primitivesFormat)

  @Benchmark
  def readExtractFieldsCirce(): ExtractFields =
    decode[ExtractFields](new String(extractFieldsJson, StandardCharsets.UTF_8)).fold(e => throw new IllegalArgumentException(e), x => x)

  @Benchmark
  def readExtractFieldsJackson(): ExtractFields = jacksonMapper.readValue[ExtractFields](extractFieldsJson)

  @Benchmark
  def readExtractFieldsJsoniter(): ExtractFields = JsonReader.read(extractFieldsCodec, extractFieldsJson)

  @Benchmark
  def readExtractFieldsPlay(): ExtractFields = Json.parse(extractFieldsJson).as[ExtractFields](extractFieldsFormat)

  @Benchmark
  def writeAnyRefsCirce(): Array[Byte] = anyRefsObj.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)

  @Benchmark
  def writeAnyRefsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(anyRefsObj)

  @Benchmark
  def writeAnyRefsJsoniter(): Array[Byte] = JsonWriter.write(anyRefsCodec, anyRefsObj)

  @Benchmark
  def writeAnyRefsPlay(): Array[Byte] = Json.toBytes(Json.toJson(anyRefsObj)(anyRefsFormat))

  @Benchmark
  def writeArraysCirce(): Array[Byte] = arraysObj.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)

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
  def writeIterablesCirce(): Array[Byte] = iterablesObj.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)

  @Benchmark
  def writeIterablesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(iterablesObj)

  @Benchmark
  def writeIterablesJsoniter(): Array[Byte] = JsonWriter.write(iterablesCodec, iterablesObj)

  @Benchmark
  def writeIterablesPlay(): Array[Byte] = Json.toBytes(Json.toJson(iterablesObj)(iterablesFormat))

  @Benchmark
  def writeMapsCirce(): Array[Byte] = mapsObj.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)

  @Benchmark
  def writeMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(mapsObj)

  @Benchmark
  def writeMapsJsoniter(): Array[Byte] = JsonWriter.write(mapsCodec, mapsObj)

  @Benchmark
  def writeMapsPlay(): Array[Byte] = Json.toBytes(Json.toJson(mapsObj)(mapsFormat))

  @Benchmark
  def writeMutableMapsCirce(): Array[Byte] = mutableMapsObj.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)

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
  def writePrimitivesCirce(): Array[Byte] = primitivesObj.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)

  @Benchmark
  def writePrimitivesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(primitivesObj)

  @Benchmark
  def writePrimitivesJsoniter(): Array[Byte] = JsonWriter.write(primitivesCodec, primitivesObj)

  @Benchmark
  def writePrimitivesPlay(): Array[Byte] = Json.toBytes(Json.toJson(primitivesObj)(primitivesFormat))
}

case class AnyRefs(s: String, bd: BigDecimal, os: Option[String])

case class Arrays(aa: Array[Array[Int]], a: Array[BigInt])

case class Iterables(l: List[String], s: Set[Int], ls: List[Set[Int]])

case class BitSets(bs: BitSet, mbs: mutable.BitSet)

case class Maps(m: HashMap[String, Double], mm: Map[Int, HashMap[Long, Double]])

case class MutableMaps(m: mutable.HashMap[String, Double], mm: mutable.Map[Int, mutable.OpenHashMap[Long, Double]])

case class IntAndLongMaps(m: IntMap[Double], mm: mutable.LongMap[LongMap[Double]])

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

case class ExtractFields(s: String, l: Long)