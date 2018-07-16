package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

//import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.macros.TwitterAPI._
import io.circe.generic.auto._
import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
//import upickle.default._

import scala.collection.immutable.Seq

class TwitterAPIBenchmark extends CommonParams {
  var obj: Seq[Tweet] = readFromArray[Seq[Tweet]](jsonBytes)
/* FIXME: AVSystem GenCodec serializes option field as JSON array and use Vector as a Seq implementation instead of List
  @Benchmark
  def readAVSystemGenCodec(): Seq[Tweet] = JsonStringInput.read[Seq[Tweet]](new String(jsonBytes, UTF_8))
*/
  @Benchmark
  def readCirce(): Seq[Tweet] = decode[Seq[Tweet]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Seq[Tweet] = jacksonMapper.readValue[Seq[Tweet]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Seq[Tweet] = readFromArray[Seq[Tweet]](jsonBytes)

  @Benchmark
  def readPlayJson(): Seq[Tweet] = Json.parse(jsonBytes).as[Seq[Tweet]](twitterAPIFormat)
/* FIXME: cannot alter uPickle to store Long as JSON number
  @Benchmark
  def readUPickle(): Seq[Tweet] = read[Seq[Tweet]](jsonBytes)
*/
/* FIXME: AVSystem GenCodec serializes option field as JSON array and serializes empty collections
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
*/
/* FIXME: circe serializes empty collections
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)
/* FIXME: Play-JSON serializes empty collections
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(twitterAPIFormat))
*/
/* FIXME: cannot alter uPickle to store Long as JSON number
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}