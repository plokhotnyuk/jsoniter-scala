package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._
import zio.json.DecoderOps

class ArrayOfUUIDsReading extends ArrayOfUUIDsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[UUID] = JsonStringInput.read[Array[UUID]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[UUID] = io.bullet.borer.Json.decode(jsonBytes).to[Array[UUID]].value

  @Benchmark
  def circe(): Array[UUID] = decode[Array[UUID]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): Array[UUID] = io.circe.jawn.decodeByteArray[Array[UUID]](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[UUID] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[UUID]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[UUID] = dslJsonDecode[Array[UUID]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[UUID] = jacksonMapper.readValue[Array[UUID]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[UUID] = readFromArray[Array[UUID]](jsonBytes)

  @Benchmark
  def playJson(): Array[UUID] = Json.parse(jsonBytes).as[Array[UUID]]

  @Benchmark
  def playJsonJsoniter(): Array[UUID] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[UUID]])

  @Benchmark
  def sprayJson(): Array[UUID] = JsonParser(jsonBytes).convertTo[Array[UUID]]

  @Benchmark
  def uPickle(): Array[UUID] = read[Array[UUID]](jsonBytes)

  @Benchmark
  def weePickle(): Array[UUID] = FromJson(jsonBytes).transform(ToScala[Array[UUID]])

  @Benchmark
  def zioJson(): Array[UUID] = new String(jsonBytes, UTF_8).fromJson[Array[UUID]].fold(sys.error, identity)
}