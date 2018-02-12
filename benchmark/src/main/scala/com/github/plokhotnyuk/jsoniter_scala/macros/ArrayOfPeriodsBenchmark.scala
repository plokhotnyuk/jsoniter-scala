package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._
import java.time.Period

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.java8.time._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class ArrayOfPeriodsBenchmark extends CommonParams {
  val obj: Array[Period] = (1 to 128).map(i => Period.of(i, i, i)).toArray
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[Period] = decode[Array[Period]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[Period] = jacksonMapper.readValue[Array[Period]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[Period] = JsonReader.read[Array[Period]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Period] = Json.parse(jsonBytes).as[Array[Period]]

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