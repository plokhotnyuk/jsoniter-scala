package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.generic.auto._
//import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

import scala.collection.mutable

case class MutableMaps(m: mutable.HashMap[String, Double],
                       @JsonDeserialize(using = classOf[CustomMutableMapDeserializer]) mm: mutable.Map[Int, mutable.OpenHashMap[Long, Double]])

class MutableMapsBenchmark extends CommonParams {
  val obj: MutableMaps = MutableMaps(mutable.HashMap("1" -> 1.1, "2" -> 2.2),
    mutable.Map(1 -> mutable.OpenHashMap(3L -> 3.3), 2 -> mutable.OpenHashMap.empty[Long, Double]))
  val jsonString: String = """{"m":{"2":2.2,"1":1.1},"mm":{"2":{},"1":{"3":3.3}}}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes

/* FIXME: Circe doesn't support parsing of mutable maps
  @Benchmark
  def readCirce(): MutableMaps = decode[MutableMaps](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
*/
  @Benchmark
  def readJacksonScala(): MutableMaps = jacksonMapper.readValue[MutableMaps](jsonBytes)

  @Benchmark
  def readJsoniterScala(): MutableMaps = readFromArray[MutableMaps](jsonBytes)

  @Benchmark
  def readPlayJson(): MutableMaps = Json.parse(jsonBytes).as[MutableMaps](mutableMapsFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/*FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(mutableMapsFormat))
}