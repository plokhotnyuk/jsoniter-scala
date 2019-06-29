package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfInstantsWriting extends ArrayOfInstantsBenchmark {
  val borerCborEncoder: io.bullet.borer.Encoder[Instant] =
    io.bullet.borer.Encoder.forTuple2[Long, Long].contramap((x: Instant) => (x.getEpochSecond, x.getNano))
  val borerJsonEncoder: io.bullet.borer.Encoder[Instant] =
    io.bullet.borer.Encoder.forString.contramap(_.toString)

  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def borerCbor(): Array[Byte] = {
    implicit val encoder: io.bullet.borer.Encoder[Instant] = borerCborEncoder
    io.bullet.borer.Json.encode(obj).toByteArray
  }

  @Benchmark
  def borerJson(): Array[Byte] = {
    implicit val encoder: io.bullet.borer.Encoder[Instant] = borerJsonEncoder
    io.bullet.borer.Json.encode(obj).toByteArray
  }

  @Benchmark
  def circe(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def playJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def sprayJson(): Array[Byte] = obj.toJson.compactPrint.getBytes(UTF_8)

  @Benchmark
  def uPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}