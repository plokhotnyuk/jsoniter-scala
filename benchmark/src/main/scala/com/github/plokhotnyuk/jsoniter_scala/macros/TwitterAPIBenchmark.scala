package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.TwitterAPI._
import io.circe.generic.auto._
import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class TwitterAPIBenchmark extends CommonParams {
  val obj: Seq[Tweet] = read[Seq[Tweet]](jsonBytes)

  @Benchmark
  def readCirce(): Seq[Tweet] = decode[Seq[Tweet]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Seq[Tweet] = jacksonMapper.readValue[Seq[Tweet]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Seq[Tweet] = read[Seq[Tweet]](jsonBytes)

  @Benchmark
  def readPlayJson(): Seq[Tweet] = Json.parse(jsonBytes).as[Seq[Tweet]](twitterAPIFormat)
/* FIXME: circe serializes empty collections
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = write(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = write(obj, preallocatedBuf, 0)
/* FIXME: Play-JSON serializes empty collections
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(twitterAPIFormat))
*/
}