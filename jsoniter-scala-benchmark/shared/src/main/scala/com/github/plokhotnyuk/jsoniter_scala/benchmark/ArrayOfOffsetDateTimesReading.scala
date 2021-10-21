package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.OffsetDateTime
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import zio.json.DecoderOps

class ArrayOfOffsetDateTimesReading extends ArrayOfOffsetDateTimesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[OffsetDateTime] = JsonStringInput.read[Array[OffsetDateTime]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[OffsetDateTime] = io.bullet.borer.Json.decode(jsonBytes).to[Array[OffsetDateTime]].value

  @Benchmark
  def circe(): Array[OffsetDateTime] = decode[Array[OffsetDateTime]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): Array[OffsetDateTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.circe.JavaTimeCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[Array[OffsetDateTime]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[OffsetDateTime] = dslJsonDecode[Array[OffsetDateTime]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[OffsetDateTime] = jacksonMapper.readValue[Array[OffsetDateTime]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[OffsetDateTime] = readFromArray[Array[OffsetDateTime]](jsonBytes)

  @Benchmark
  def playJson(): Array[OffsetDateTime] = Json.parse(jsonBytes).as[Array[OffsetDateTime]]

  @Benchmark
  def playJsonJsoniter(): Array[OffsetDateTime] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[OffsetDateTime]])

  @Benchmark
  def sprayJson(): Array[OffsetDateTime] = JsonParser(jsonBytes).convertTo[Array[OffsetDateTime]]

  @Benchmark
  def uPickle(): Array[OffsetDateTime] = read[Array[OffsetDateTime]](jsonBytes)

  @Benchmark
  def weePickle(): Array[OffsetDateTime] = FromJson(jsonBytes).transform(ToScala[Array[OffsetDateTime]])

  @Benchmark
  def zioJson(): Array[OffsetDateTime] =
    new String(jsonBytes, UTF_8).fromJson[Array[OffsetDateTime]].fold(sys.error, identity)
}