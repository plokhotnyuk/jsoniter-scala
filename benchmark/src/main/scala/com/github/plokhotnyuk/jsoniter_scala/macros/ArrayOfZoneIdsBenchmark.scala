package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._
import java.time.ZoneId

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.java8.time._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import scala.collection.JavaConverters._

class ArrayOfZoneIdsBenchmark extends CommonParams {
  val obj: Array[ZoneId] = (ZoneId.getAvailableZoneIds.asScala.take(100).map(ZoneId.of) ++
    (1 to 7).map(i => ZoneId.of(s"+0$i:0$i:0$i")) ++
    (1 to 7).map(i => ZoneId.of(s"UT+0$i:0$i:0$i")) ++
    (1 to 7).map(i => ZoneId.of(s"UTC+0$i:0$i:0$i")) ++
    (1 to 7).map(i => ZoneId.of(s"GMT+0$i:0$i:0$i"))).toArray // 128 ZoneIds
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[ZoneId] = decode[Array[ZoneId]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[ZoneId] = jacksonMapper.readValue[Array[ZoneId]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[ZoneId] = JsonReader.read(zoneIdArrayCodec, jsonBytes)

  @Benchmark
  def readPlayJson(): Array[ZoneId] = Json.parse(jsonBytes).as[Array[ZoneId]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = JsonWriter.write(zoneIdArrayCodec, obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = JsonWriter.write(zoneIdArrayCodec, obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}