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

class ArrayOfDoublesBenchmark extends CommonParams {
  val obj: Array[Double] = (1 to 128)
    .map(i => ((i * 372036854775807L) / Math.pow(10, i % 19)).toLong * Math.pow(10, (i % 38) - 19)).toArray
  val jsonString: String = obj.mkString("[", ",", "]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[Double] = decode[Array[Double]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[Double] = jacksonMapper.readValue[Array[Double]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[Double] = JsonReader.read[Array[Double]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Double] = Json.parse(jsonBytes).as[Array[Double]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = JsonWriter.write(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = JsonWriter.write(obj, preallocatedBuf, 0)
/* FIXME: Play serializes doubles in different format than toString: 0.0 as 0, 7.0687002407403325E18 as 7068700240740332500
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
}