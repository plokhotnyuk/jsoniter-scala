package com.github.plokhotnyuk.jsoniter_scala.macros

//import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import io.circe.generic.auto._
//import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

import scala.collection.immutable.{IntMap, LongMap}
import scala.collection.mutable

case class IntAndLongMaps(m: IntMap[Double], mm: mutable.LongMap[LongMap[Double]])

class IntAndLongMapsBenchmark extends CommonParams {
  val obj: IntAndLongMaps = IntAndLongMaps(IntMap(1 -> 1.1, 2 -> 2.2),
    mutable.LongMap(1L -> LongMap(3L -> 3.3), 2L -> LongMap.empty[Double]))
  val jsonString: String = """{"m":{"1":1.1,"2":2.2},"mm":{"2":{},"1":{"3":3.3}}}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes

/* FIXME: Circe doesn't support parsing of int & long maps
  @Benchmark
  def readCirce(): IntAndLongMaps = decode[IntAndLongMaps](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
*/

/* FIXME: Jackson-module-scala doesn't support parsing of int & long maps
  @Benchmark
  def readJackson(): IntAndLongMaps = jacksonMapper.readValue[IntAndLongMaps](jsonBytes)
*/

  @Benchmark
  def readJsoniter(): IntAndLongMaps = JsonReader.read(intAndLongMapsCodec, jsonBytes)

  @Benchmark
  def readPlay(): IntAndLongMaps = Json.parse(jsonBytes).as[IntAndLongMaps](intAndLongMapsFormat)

  /* FIXME: Circe doesn't support writing of int & long maps
    @Benchmark
    def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
  */

  @Benchmark
  def writeJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniter(): Array[Byte] = JsonWriter.write(intAndLongMapsCodec, obj)

  @Benchmark
  def writePlay(): Array[Byte] = Json.toBytes(Json.toJson(obj)(intAndLongMapsFormat))
}