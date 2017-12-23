package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
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
  "-XX:+AlwaysPreTouch",
  "-XX:+UseParallelGC"
))
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class JsonCodecMakerBenchmark {
  val missingReqFieldJsonString: String = """{}"""
  val missingReqFieldJsonBytes: Array[Byte] = missingReqFieldJsonString.getBytes
  val anyRefsObj: AnyRefs = AnyRefs("s", 1, Some("os"))
  val anyRefsJsonString: String = """{"s":"s","bd":1,"os":"os"}"""
  val anyRefsJsonBytes: Array[Byte] = anyRefsJsonString.getBytes
  val arraysObj: Arrays = Arrays(Array(Array(1, 2, 3), Array(4, 5, 6)), Array(BigInt(7)))
  val arraysJsonString: String = """{"aa":[[1,2,3],[4,5,6]],"a":[7]}"""
  val arraysJsonBytes: Array[Byte] = arraysJsonString.getBytes
  val bitSetsObj: BitSets = BitSets(BitSet(1, 2, 3), mutable.BitSet(4, 5, 6))
  val bitSetsJsonString: String = """{"bs":[1,2,3],"mbs":[4,5,6]}"""
  val bitSetsJsonBytes: Array[Byte] = bitSetsJsonString.getBytes
  val intArrayObj: Array[Int] = (1 to 1000).map(i => ((i * 1498724053) / Math.pow(10, i % 10)).toInt).toArray
  val intArrayJsonString: String = intArrayObj.mkString("[", ",", "]")
  val intArrayJsonBytes: Array[Byte] = intArrayJsonString.getBytes
  val iterablesObj: Iterables = Iterables(Vector("1", "2", "3"), Set(4, 5, 6), List(HashSet(1, 2), HashSet()))
  val iterablesJsonString: String = """{"l":["1","2","3"],"s":[4,5,6],"ls":[[1,2],[]]}"""
  val iterablesJsonBytes: Array[Byte] = iterablesJsonString.getBytes
  val mutableIterablesObj: MutableIterables = MutableIterables(mutable.ArrayBuffer("1", "2", "3"), mutable.TreeSet(4, 5, 6),
    mutable.ResizableArray(mutable.Set(1, 2), mutable.Set()))
  val mutableIterablesJsonString: String = """{"l":["1","2","3"],"s":[4,5,6],"ls":[[1,2],[]]}"""
  val mutableIterablesJsonBytes: Array[Byte] = mutableIterablesJsonString.getBytes
  val mapsObj: Maps = Maps(HashMap("1" -> 1.1, "2" -> 2.2), Map(1 -> HashMap(3L -> 3.3), 2 -> HashMap.empty[Long, Double]))
  val mapsJsonString: String = """{"m":{"1":1.1,"2":2.2},"mm":{"1":{"3":3.3},"2":{}}}"""
  val mapsJsonBytes: Array[Byte] = mapsJsonString.getBytes
  val mutableMapsObj: MutableMaps = MutableMaps(mutable.HashMap("1" -> 1.1, "2" -> 2.2),
    mutable.Map(1 -> mutable.OpenHashMap(3L -> 3.3), 2 -> mutable.OpenHashMap.empty[Long, Double]))
  val mutableMapsJsonString: String = """{"m":{"2":2.2,"1":1.1},"mm":{"2":{},"1":{"3":3.3}}}"""
  val mutableMapsJsonBytes: Array[Byte] = mutableMapsJsonString.getBytes
  val intAndLongMapsObj: IntAndLongMaps = IntAndLongMaps(IntMap(1 -> 1.1, 2 -> 2.2),
    mutable.LongMap(1L -> LongMap(3L -> 3.3), 2L -> LongMap.empty[Double]))
  val intAndLongMapsJsonString: String = """{"m":{"1":1.1,"2":2.2},"mm":{"2":{},"1":{"3":3.3}}}"""
  val intAndLongMapsJsonBytes: Array[Byte] = intAndLongMapsJsonString.getBytes
  val primitivesObj: Primitives = Primitives(1, 2, 3, 4, bl = true, ch = 'x', 1.1, 2.5f)
  val primitivesJsonString: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":1.1,"f":2.5}"""
  val primitivesJsonBytes: Array[Byte] = primitivesJsonString.getBytes
  val extractFieldsObj: ExtractFields = ExtractFields("s", 1L)
  val extractFieldsJsonString: String =
    """{"i1":["1","2"],"s":"s","i2":{"m":[[1,2],[3,4]],"f":true},"l":1,"i3":{"1":1.1,"2":2.2}}"""
  val extractFieldsJsonBytes: Array[Byte] = extractFieldsJsonString.getBytes
  val adtObj: AdtBase = C(A(1), B("VVV"))
  val adtJsonString: String = """{"type":"C","l":{"type":"A","a":1},"r":{"type":"B","b":"VVV"}}"""
  val adtJsonBytes: Array[Byte] = adtJsonString.getBytes
  val asciiStringObj: String =
    "In computer science, an inverted index (also referred to as postings file or inverted file) is an index data structure storing a mapping from content, such as words or numbers, to its locations in a database file, or in a document or a set of documents (named in contrast to a Forward Index, which maps from documents to content). The purpose of an inverted index is to allow fast full text searches, at a cost of increased processing when a document is added to the database. The inverted file may be the database file itself, rather than its index. It is the most popular data structure used in document retrieval systems,[1] used on a large scale for example in search engines. Additionally, several significant general-purpose mainframe-based database management systems have used inverted list architectures, including ADABAS, DATACOM/DB, and Model 204. There are two main variants of inverted indexes: A record-level inverted index (or inverted file index or just inverted file) contains a list of references to documents for each word. A word-level inverted index (or full inverted index or inverted list) additionally contains the positions of each word within a document. The latter form offers more functionality (like phrase searches), but needs more processing power and space to be created."
  val asciiStringJsonString: String =
    """"In computer science, an inverted index (also referred to as postings file or inverted file) is an index data structure storing a mapping from content, such as words or numbers, to its locations in a database file, or in a document or a set of documents (named in contrast to a Forward Index, which maps from documents to content). The purpose of an inverted index is to allow fast full text searches, at a cost of increased processing when a document is added to the database. The inverted file may be the database file itself, rather than its index. It is the most popular data structure used in document retrieval systems,[1] used on a large scale for example in search engines. Additionally, several significant general-purpose mainframe-based database management systems have used inverted list architectures, including ADABAS, DATACOM/DB, and Model 204. There are two main variants of inverted indexes: A record-level inverted index (or inverted file index or just inverted file) contains a list of references to documents for each word. A word-level inverted index (or full inverted index or inverted list) additionally contains the positions of each word within a document. The latter form offers more functionality (like phrase searches), but needs more processing power and space to be created.""""
  val asciiStringJsonBytes: Array[Byte] = asciiStringJsonString.getBytes("UTF-8")
  val nonAsciiStringObj: String =
    "倒排索引（英语：Inverted index），也常被称为反向索引、置入档案或反向档案，是一种索引方法，被用来存储在全文搜索下某个单词在一个文档或者一组文档中的存储位置的映射。它是文档检索系统中最常用的数据结构。"
  val nonAsciiStringJsonString: String =
    """"倒排索引（英语：Inverted index），也常被称为反向索引、置入档案或反向档案，是一种索引方法，被用来存储在全文搜索下某个单词在一个文档或者一组文档中的存储位置的映射。它是文档检索系统中最常用的数据结构。""""
  val nonAsciiStringJsonBytes: Array[Byte] = nonAsciiStringJsonString.getBytes("UTF-8")
  val googleMapsAPIObj: DistanceMatrix = JsonReader.read(googleMapsAPICodec, GoogleMapsAPI.jsonBytes)
  val twitterAPIObj: Seq[Tweet] = JsonReader.read(twitterAPICodec, TwitterAPI.jsonBytes)

  @Benchmark
  def missingReqFieldCirce(): String =
    decode[MissingReqFields](new String(missingReqFieldJsonBytes, UTF_8)).fold(_.getMessage, _ => null)

  @Benchmark
  def missingReqFieldJackson(): String =
    try {
      jacksonMapper.readValue[MissingReqFields](missingReqFieldJsonBytes).toString // toString() should not be called
    } catch {
      case ex: MismatchedInputException => ex.getMessage
    }

  @Benchmark
  def missingReqFieldJsoniter(): String =
    try {
      JsonReader.read(missingReqFieldCodec, missingReqFieldJsonBytes).toString // toString() should not be called
    } catch {
      case ex: JsonParseException => ex.getMessage
    }

  @Benchmark
  def missingReqFieldJsoniterStackless(): String =
    try {
      JsonReader.read(missingReqFieldCodec, missingReqFieldJsonBytes, stacklessExceptionConfig).toString // toString() should not be called
    } catch {
      case ex: JsonParseException => ex.getMessage
    }

  @Benchmark
  def missingReqFieldJsoniterStacklessNoDump(): String =
    try {
      JsonReader.read(missingReqFieldCodec, missingReqFieldJsonBytes, stacklessExceptionWithoutDumpConfig).toString // toString() should not be called
    } catch {
      case ex: JsonParseException => ex.getMessage
    }

  @Benchmark
  def missingReqFieldPlay(): String =
    try {
      Json.parse(missingReqFieldJsonBytes).as[MissingReqFields](missingReqFieldFormat).toString // toString() should not be called
    } catch {
      case ex: JsResultException => ex.getMessage
    }

  @Benchmark
  def readAnyRefsCirce(): AnyRefs = decode[AnyRefs](new String(anyRefsJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readAnyRefsJackson(): AnyRefs = jacksonMapper.readValue[AnyRefs](anyRefsJsonBytes)

  @Benchmark
  def readAnyRefsJsoniter(): AnyRefs = JsonReader.read(anyRefsCodec, anyRefsJsonBytes)

  @Benchmark
  def readAnyRefsPlay(): AnyRefs = Json.parse(anyRefsJsonBytes).as[AnyRefs](anyRefsFormat)

  @Benchmark
  def readArraysCirce(): Arrays = decode[Arrays](new String(arraysJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readArraysJackson(): Arrays = jacksonMapper.readValue[Arrays](arraysJsonBytes)

  @Benchmark
  def readArraysJsoniter(): Arrays = JsonReader.read(arraysCodec, arraysJsonBytes)

  @Benchmark
  def readArraysPlay(): Arrays = Json.parse(arraysJsonBytes).as[Arrays](arraysFormat)

/* FIXME: Circe doesn't support parsing of bitsets
  @Benchmark
  def readBitSetsCirce(): BitSets = decode[BitSets](new String(bitSetsJsonBytes, UTF_8)).fold(throw _, x => x)
*/

  @Benchmark
  def readBitSetsJackson(): BitSets = jacksonMapper.readValue[BitSets](bitSetsJsonBytes)

  @Benchmark
  def readBitSetsJsoniter(): BitSets = JsonReader.read(bitSetsCodec, bitSetsJsonBytes)

  @Benchmark
  def readBitSetsPlay(): BitSets = Json.parse(bitSetsJsonBytes).as[BitSets](bitSetsFormat)

  @Benchmark
  def readIntArrayCirce(): Array[Int] = decode[Array[Int]](new String(intArrayJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readIntArrayJackson(): Array[Int] = jacksonMapper.readValue[Array[Int]](intArrayJsonBytes)

  @Benchmark
  def readIntArrayJsoniter(): Array[Int] = JsonReader.read(intArrayCodec, intArrayJsonBytes)

  @Benchmark
  def readIntArrayPlay(): Array[Int] = Json.parse(intArrayJsonBytes).as[Array[Int]]

  @Benchmark
  def readIterablesCirce(): Iterables = decode[Iterables](new String(iterablesJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readIterablesJackson(): Iterables = jacksonMapper.readValue[Iterables](iterablesJsonBytes)

  @Benchmark
  def readIterablesJsoniter(): Iterables = JsonReader.read(iterablesCodec, iterablesJsonBytes)

  @Benchmark
  def readIterablesPlay(): Iterables = Json.parse(iterablesJsonBytes).as[Iterables](iterablesFormat)

  @Benchmark
  def readMutableIterablesCirce(): MutableIterables = decode[MutableIterables](new String(mutableIterablesJsonBytes, UTF_8)).fold(throw _, x => x)

/* FIXME: Jackson-module-scala doesn't support parsing of tree sets
  @Benchmark
  def readMutableIterablesJackson(): MutableIterables = jacksonMapper.readValue[MutableIterables](mutableIterablesJsonBytes)
*/

  @Benchmark
  def readMutableIterablesJsoniter(): MutableIterables = JsonReader.read(mutableIterablesCodec, mutableIterablesJsonBytes)

  @Benchmark
  def readMutableIterablesPlay(): MutableIterables = Json.parse(mutableIterablesJsonBytes).as[MutableIterables](mutableIterablesFormat)

  @Benchmark
  def readMapsCirce(): Maps = decode[Maps](new String(mapsJsonBytes, UTF_8)) .fold(throw _, x => x)

  @Benchmark
  def readMapsJackson(): Maps = jacksonMapper.readValue[Maps](mapsJsonBytes)

  @Benchmark
  def readMapsJsoniter(): Maps = JsonReader.read(mapsCodec, mapsJsonBytes)

  @Benchmark
  def readMapsPlay(): Maps = Json.parse(mapsJsonBytes).as[Maps](mapsFormat)

/* FIXME: Circe doesn't support parsing of mutable maps
  @Benchmark
  def readMutableMapsCirce(): MutableMaps = decode[MutableMaps](new String(mutableMapsJsonBytes, UTF_8)).fold(throw _, x => x)
*/

  @Benchmark
  def readMutableMapsJackson(): MutableMaps = jacksonMapper.readValue[MutableMaps](mutableMapsJsonBytes)

  @Benchmark
  def readMutableMapsJsoniter(): MutableMaps = JsonReader.read(mutableMapsCodec, mutableMapsJsonBytes)

  @Benchmark
  def readMutableMapsPlay(): MutableMaps = Json.parse(mutableMapsJsonBytes).as[MutableMaps](mutableMapsFormat)

/* FIXME: Circe doesn't support parsing of int & long maps
  @Benchmark
  def readIntAndLongMapsCirce(): IntAndLongMaps = decode[IntAndLongMaps](new String(intAndLongMapsJsonBytes, UTF_8)).fold(throw _, x => x)
*/

/* FIXME: Jackson-module-scala doesn't support parsing of int & long maps
  @Benchmark
  def readIntAndLongMapsJackson(): IntAndLongMaps = jacksonMapper.readValue[IntAndLongMaps](intAndLongMapsJsonBytes)
*/

  @Benchmark
  def readIntAndLongMapsJsoniter(): IntAndLongMaps = JsonReader.read(intAndLongMapsCodec, intAndLongMapsJsonBytes)

  @Benchmark
  def readIntAndLongMapsPlay(): IntAndLongMaps = Json.parse(intAndLongMapsJsonBytes).as[IntAndLongMaps](intAndLongMapsFormat)

  @Benchmark
  def readPrimitivesCirce(): Primitives = decode[Primitives](new String(primitivesJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readPrimitivesJackson(): Primitives = jacksonMapper.readValue[Primitives](primitivesJsonBytes)

  @Benchmark
  def readPrimitivesJsoniter(): Primitives = JsonReader.read(primitivesCodec, primitivesJsonBytes)

  @Benchmark
  def readPrimitivesPlay(): Primitives = Json.parse(primitivesJsonBytes).as[Primitives](primitivesFormat)

  @Benchmark
  def readExtractFieldsCirce(): ExtractFields = decode[ExtractFields](new String(extractFieldsJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readExtractFieldsJackson(): ExtractFields = jacksonMapper.readValue[ExtractFields](extractFieldsJsonBytes)

  @Benchmark
  def readExtractFieldsJsoniter(): ExtractFields = JsonReader.read(extractFieldsCodec, extractFieldsJsonBytes)

  @Benchmark
  def readExtractFieldsPlay(): ExtractFields = Json.parse(extractFieldsJsonBytes).as[ExtractFields](extractFieldsFormat)

  @Benchmark
  def readAdtCirce(): AdtBase = decode[AdtBase](new String(adtJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readAdtJackson(): AdtBase = jacksonMapper.readValue[AdtBase](adtJsonBytes)

  @Benchmark
  def readAdtJsoniter(): AdtBase = JsonReader.read(adtCodec, adtJsonBytes)

  @Benchmark
  def readAdtPlay(): AdtBase = Json.parse(adtJsonBytes).as[AdtBase](adtFormat)

  @Benchmark
  def readAsciiStringCirce(): String = decode[String](new String(asciiStringJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readAsciiStringJackson(): String = jacksonMapper.readValue[String](asciiStringJsonBytes)

  @Benchmark
  def readAsciiStringJsoniter(): String = JsonReader.read(stringCodec, asciiStringJsonBytes)

/* FIXME: find proper way to parse string value in Play JSON
  @Benchmark
  def readAsciiStringPlay(): String = Json.parse(asciiStringJson).toString()
*/
  @Benchmark
  def readNonAsciiStringCirce(): String = decode[String](new String(nonAsciiStringJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readNonAsciiStringJackson(): String = jacksonMapper.readValue[String](nonAsciiStringJsonBytes)

  @Benchmark
  def readNonAsciiStringJsoniter(): String = JsonReader.read(stringCodec, nonAsciiStringJsonBytes)

/* FIXME: find proper way to parse string value in Play JSON
  @Benchmark
  def readNonAsciiStringPlay(): String = Json.parse(nonAsciiStringJson).toString()
*/
  @Benchmark
  def readGoogleMapsAPICirce(): DistanceMatrix = decode[DistanceMatrix](new String(GoogleMapsAPI.jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readGoogleMapsAPIJackson(): DistanceMatrix = jacksonMapper.readValue[DistanceMatrix](GoogleMapsAPI.jsonBytes)

  @Benchmark
  def readGoogleMapsAPIJsoniter(): DistanceMatrix = JsonReader.read(googleMapsAPICodec, GoogleMapsAPI.jsonBytes)

  @Benchmark
  def readGoogleMapsAPIPlay(): DistanceMatrix = Json.parse(GoogleMapsAPI.jsonBytes).as[DistanceMatrix](googleMapsAPIFormat)

  @Benchmark
  def readTwitterAPICirce(): Seq[Tweet] = decode[Seq[Tweet]](new String(TwitterAPI.jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readTwitterAPIJackson(): Seq[Tweet] = jacksonMapper.readValue[Seq[Tweet]](TwitterAPI.jsonBytes)

  @Benchmark
  def readTwitterAPIJsoniter(): Seq[Tweet] = JsonReader.read(twitterAPICodec, TwitterAPI.jsonBytes)

  @Benchmark
  def readTwitterAPIPlay(): Seq[Tweet] = Json.parse(TwitterAPI.jsonBytes).as[Seq[Tweet]](twitterAPIFormat)

  @Benchmark
  def writeAnyRefsCirce(): Array[Byte] = printer.pretty(anyRefsObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeAnyRefsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(anyRefsObj)

  @Benchmark
  def writeAnyRefsJsoniter(): Array[Byte] = JsonWriter.write(anyRefsCodec, anyRefsObj)

  @Benchmark
  def writeAnyRefsJsoniterPrealloc(): Int = JsonWriter.write(anyRefsCodec, anyRefsObj, preallocatedBuf.get, 0)

  @Benchmark
  def writeAnyRefsPlay(): Array[Byte] = Json.toBytes(Json.toJson(anyRefsObj)(anyRefsFormat))

  @Benchmark
  def writeArraysCirce(): Array[Byte] = printer.pretty(arraysObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeArraysJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(arraysObj)

  @Benchmark
  def writeArraysJsoniter(): Array[Byte] = JsonWriter.write(arraysCodec, arraysObj)

  @Benchmark
  def writeArraysPlay(): Array[Byte] = Json.toBytes(Json.toJson(arraysObj)(arraysFormat))

/* FIXME: Circe doesn't support writing of bitsets
  @Benchmark
  def writeBitSetsCirce(): Array[Byte] = printer.pretty(bitSetsObj.asJson).getBytes(UTF_8)
*/

  @Benchmark
  def writeBitSetsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(bitSetsObj)

  @Benchmark
  def writeBitSetsJsoniter(): Array[Byte] = JsonWriter.write(bitSetsCodec, bitSetsObj)

  @Benchmark
  def writeBitSetsPlay(): Array[Byte] = Json.toBytes(Json.toJson(bitSetsObj)(bitSetsFormat))

  @Benchmark
  def writeIntArrayCirce(): Array[Byte] = printer.pretty(intArrayObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeIntArrayJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(intArrayObj)

  @Benchmark
  def writeIntArrayJsoniter(): Array[Byte] = JsonWriter.write(intArrayCodec, intArrayObj)

  @Benchmark
  def writeIntArrayJsoniterPrealloc(): Int = JsonWriter.write(intArrayCodec, intArrayObj, preallocatedBuf.get, 0)

  @Benchmark
  def writeIntArrayPlay(): Array[Byte] = Json.toBytes(Json.toJson(intArrayObj))

  @Benchmark
  def writeIterablesCirce(): Array[Byte] = printer.pretty(iterablesObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeIterablesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(iterablesObj)

  @Benchmark
  def writeIterablesJsoniter(): Array[Byte] = JsonWriter.write(iterablesCodec, iterablesObj)

  @Benchmark
  def writeIterablesPlay(): Array[Byte] = Json.toBytes(Json.toJson(iterablesObj)(iterablesFormat))

  @Benchmark
  def writeMutableIterablesCirce(): Array[Byte] = printer.pretty(mutableIterablesObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeMutableIterablesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(mutableIterablesObj)

  @Benchmark
  def writeMutableIterablesJsoniter(): Array[Byte] = JsonWriter.write(mutableIterablesCodec, mutableIterablesObj)

  @Benchmark
  def writeMutableIterablesPlay(): Array[Byte] = Json.toBytes(Json.toJson(mutableIterablesObj)(mutableIterablesFormat))

  @Benchmark
  def writeMapsCirce(): Array[Byte] = printer.pretty(mapsObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(mapsObj)

  @Benchmark
  def writeMapsJsoniter(): Array[Byte] = JsonWriter.write(mapsCodec, mapsObj)

  @Benchmark
  def writeMapsPlay(): Array[Byte] = Json.toBytes(Json.toJson(mapsObj)(mapsFormat))

  @Benchmark
  def writeMutableMapsCirce(): Array[Byte] = printer.pretty(mutableMapsObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeMutableMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(mutableMapsObj)

  @Benchmark
  def writeMutableMapsJsoniter(): Array[Byte] = JsonWriter.write(mutableMapsCodec, mutableMapsObj)

  @Benchmark
  def writeMutableMapsPlay(): Array[Byte] = Json.toBytes(Json.toJson(mutableMapsObj)(mutableMapsFormat))

  /* FIXME: Circe doesn't support writing of int & long maps
    @Benchmark
    def writeIntAndLongMapsCirce(): Array[Byte] = printer.pretty(intAndLongMapsObj.asJson).getBytes(UTF_8)
  */

  @Benchmark
  def writeIntAndLongMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(intAndLongMapsObj)

  @Benchmark
  def writeIntAndLongMapsJsoniter(): Array[Byte] = JsonWriter.write(intAndLongMapsCodec, intAndLongMapsObj)

  @Benchmark
  def writeIntAndLongMapsPlay(): Array[Byte] = Json.toBytes(Json.toJson(intAndLongMapsObj)(intAndLongMapsFormat))

  @Benchmark
  def writePrimitivesCirce(): Array[Byte] = printer.pretty(primitivesObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writePrimitivesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(primitivesObj)

  @Benchmark
  def writePrimitivesJsoniter(): Array[Byte] = JsonWriter.write(primitivesCodec, primitivesObj)

  @Benchmark
  def writePrimitivesJsoniterPrealloc(): Int = JsonWriter.write(primitivesCodec, primitivesObj, preallocatedBuf.get, 0)

  @Benchmark
  def writePrimitivesPlay(): Array[Byte] = Json.toBytes(Json.toJson(primitivesObj)(primitivesFormat))

  @Benchmark
  def writeAdtCirce(): Array[Byte] = printer.pretty(adtObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeAdtJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(adtObj)

  @Benchmark
  def writeAdtJsoniter(): Array[Byte] = JsonWriter.write(adtCodec, adtObj)

  @Benchmark
  def writeAdtPlay(): Array[Byte] = Json.toBytes(Json.toJson(adtObj)(adtFormat))

  @Benchmark
  def writeAsciiStringCirce(): Array[Byte] = printer.pretty(asciiStringObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeAsciiStringJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(asciiStringObj)

  @Benchmark
  def writeAsciiStringJsoniter(): Array[Byte] = JsonWriter.write(stringCodec, asciiStringObj)

  @Benchmark
  def writeAsciiStringJsoniterPrealloc(): Int = JsonWriter.write(stringCodec, asciiStringObj, preallocatedBuf.get, 0)

  @Benchmark
  def writeAsciiStringPlay(): Array[Byte] = Json.toBytes(Json.toJson(asciiStringObj))

  @Benchmark
  def writeNonAsciiStringCirce(): Array[Byte] = printer.pretty(nonAsciiStringObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeNonAsciiStringJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(nonAsciiStringObj)

  @Benchmark
  def writeNonAsciiStringJsoniter(): Array[Byte] = JsonWriter.write(stringCodec, nonAsciiStringObj)

  @Benchmark
  def writeNonAsciiStringJsoniterPrealloc(): Int = JsonWriter.write(stringCodec, nonAsciiStringObj, preallocatedBuf.get, 0)

  @Benchmark
  def writeNonAsciiStringPlay(): Array[Byte] = Json.toBytes(Json.toJson(nonAsciiStringObj))

  @Benchmark
  def writeGoogleMapsAPICirce(): Array[Byte] = printer.pretty(googleMapsAPIObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeGoogleMapsAPIJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(googleMapsAPIObj)

  @Benchmark
  def writeGoogleMapsAPIJsoniter(): Array[Byte] = JsonWriter.write(googleMapsAPICodec, googleMapsAPIObj)

  @Benchmark
  def writeGoogleMapsAPIJsoniterPrealloc(): Int = JsonWriter.write(googleMapsAPICodec, googleMapsAPIObj, preallocatedBuf.get, 0)

  @Benchmark
  def writeGoogleMapsAPIPlay(): Array[Byte] = Json.toBytes(Json.toJson(googleMapsAPIObj)(googleMapsAPIFormat))

  @Benchmark
  def writeTwitterAPICirce(): Array[Byte] = printer.pretty(twitterAPIObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeTwitterAPIJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(twitterAPIObj)

  @Benchmark
  def writeTwitterAPIJsoniter(): Array[Byte] = JsonWriter.write(twitterAPICodec, twitterAPIObj)

  @Benchmark
  def writeTwitterAPIJsoniterPrealloc(): Int = JsonWriter.write(twitterAPICodec, twitterAPIObj, preallocatedBuf.get, 0)

  @Benchmark
  def writeTwitterAPIPlay(): Array[Byte] = Json.toBytes(Json.toJson(twitterAPIObj)(twitterAPIFormat))
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
