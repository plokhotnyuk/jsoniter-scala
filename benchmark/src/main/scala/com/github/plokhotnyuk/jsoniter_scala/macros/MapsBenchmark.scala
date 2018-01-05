package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

import scala.collection.immutable.{HashMap, Map}

case class Maps(m: HashMap[String, Double], mm: Map[Int, HashMap[Long, Double]])

class MapsBenchmark extends CommonParams {
  val obj: Maps = Maps(HashMap("1" -> 1.1, "2" -> 2.2), Map(1 -> HashMap(3L -> 3.3), 2 -> HashMap.empty[Long, Double]))
  val jsonString: String = """{"m":{"1":1.1,"2":2.2},"mm":{"1":{"3":3.3},"2":{}}}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Maps = decode[Maps](new String(jsonBytes, UTF_8)) .fold(throw _, x => x)
/*FIXME: Jackson-module-scala parse keys as String
  @Benchmark
  def readJackson(): Maps = jacksonMapper.readValue[Maps](jsonBytes)
*/
  @Benchmark
  def readJsoniter(): Maps = JsonReader.read(mapsCodec, jsonBytes)

  @Benchmark
  def readPlay(): Maps = Json.parse(jsonBytes).as[Maps](mapsFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/*FIXME: Jackson doesn't store key value pair when value is empty and `SerializationInclusion` set to `Include.NON_EMPTY`
  @Benchmark
  def writeJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/
  @Benchmark
  def writeJsoniter(): Array[Byte] = JsonWriter.write(mapsCodec, obj)

  @Benchmark
  def writePlay(): Array[Byte] = Json.toBytes(Json.toJson(obj)(mapsFormat))
}