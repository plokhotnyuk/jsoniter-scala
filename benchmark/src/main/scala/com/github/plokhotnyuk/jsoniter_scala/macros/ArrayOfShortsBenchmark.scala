package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class ArrayOfShortsBenchmark extends CommonParams {
  val obj: Array[Short] = (1 to 128).map(i => (i * 64).toShort).toArray
  val jsonString: String = obj.mkString("[", ",", "]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[Short] = decode[Array[Short]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[Short] = jacksonMapper.readValue[Array[Short]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[Short] = JsonReader.read[Array[Short]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Short] = Json.parse(jsonBytes).as[Array[Short]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = JsonWriter.write(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = JsonWriter.write(obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}