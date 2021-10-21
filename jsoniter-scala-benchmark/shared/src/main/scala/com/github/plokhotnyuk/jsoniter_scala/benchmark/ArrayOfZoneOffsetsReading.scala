package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.ZoneOffset
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import zio.json.DecoderOps

class ArrayOfZoneOffsetsReading extends ArrayOfZoneOffsetsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[ZoneOffset] = JsonStringInput.read[Array[ZoneOffset]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[ZoneOffset] = io.bullet.borer.Json.decode(jsonBytes).to[Array[ZoneOffset]].value

  @Benchmark
  def circe(): Array[ZoneOffset] = decode[Array[ZoneOffset]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[ZoneOffset] = {
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[ZoneOffset]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[ZoneOffset] = jacksonMapper.readValue[Array[ZoneOffset]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[ZoneOffset] = readFromArray[Array[ZoneOffset]](jsonBytes)

  @Benchmark
  def playJson(): Array[ZoneOffset] = Json.parse(jsonBytes).as[Array[ZoneOffset]]

  @Benchmark
  def playJsonJsoniter(): Array[ZoneOffset] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[ZoneOffset]])

  @Benchmark
  def sprayJson(): Array[ZoneOffset] = JsonParser(jsonBytes).convertTo[Array[ZoneOffset]]

  @Benchmark
  def uPickle(): Array[ZoneOffset] = read[Array[ZoneOffset]](jsonBytes)

  @Benchmark
  def weePickle(): Array[ZoneOffset] = FromJson(jsonBytes).transform(ToScala[Array[ZoneOffset]])

  @Benchmark
  def zioJson(): Array[ZoneOffset] = new String(jsonBytes, UTF_8).fromJson[Array[ZoneOffset]].fold(sys.error, identity)
}