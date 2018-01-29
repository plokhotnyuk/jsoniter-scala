package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._
import java.time.Instant

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.java8.time._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class ArrayOfInstantsBenchmark extends CommonParams {
  val obj: Array[Instant] = (1 to 128).map(i => Instant.ofEpochMilli(i * 15170809790L)).toArray
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[Instant] = decode[Array[Instant]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[Instant] = jacksonMapper.readValue[Array[Instant]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[Instant] = JsonReader.read(instantArrayCodec, jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Instant] = Json.parse(jsonBytes).as[Array[Instant]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

/* FIXME Jackson serializes in format <epoch seconds>.<nanos>
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = JsonWriter.write(instantArrayCodec, obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = JsonWriter.write(instantArrayCodec, obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}